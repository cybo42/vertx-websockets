

var eb = null;

function connectToServer(){

    if(!eb){
      eb = new vertx.EventBus('/eventbus');
    }

    eb.onopen = function() {
      console.log("socket opened");
      $('#connection-status').removeClass("label-important").toggleClass("label-success").text('connected');

      eb.registerHandler('price.up', function(message) {
        console.log('received a message: ' + JSON.stringify(message));
        addOrUpdateRow(message);
      });

      eb.registerHandler('activty.log', function(message) {
        console.log('received a message: ' + JSON.stringify(message));
      });

      eb.registerHandler('feed.status', function(message) {
        console.log('received a message: ' + JSON.stringify(message));
        $('#feed').html(message.message);
      });      
  }

  eb.onclose = function() {
    console.log("socket closed");
    $('#connection-status').removeClass("label-success").addClass("label-important").text('connection down');
    eb = null;
  }
}

function closeConnection(){
    if(eb){
        eb.close();
    }
}

function sendTrade(message){
    console.log("sending message");
    eb.send('send.trade', {name: 'joe', message: message}, function(reply){
        console.log("My reply was: " + JSON.stringify(reply));
    });
}

function addOrUpdateRow(trade){
    var tbl = $("#portfolio"), color = "info";

    color = (trade.price > trade.last)? "success": "error";

    var row = $('<tr></tr>').attr({id: trade.name,  class: color});

    $('<td></td>').text(trade.name).appendTo(row);
    $('<td></td>').text(trade.price).appendTo(row);
    $('<td></td>').text(trade.price - trade.last).appendTo(row);
    $('<td></td>').text(trade.ts).appendTo(row);

    var updateRow = $('#portfolio #' + trade.name);

    if(updateRow.length){
        console.log("Found updating row");
        updateRow.replaceWith(row);

    }else{
        console.log("NOT Found adding row");
        row.last().appendTo(tbl);
    }

    console.log(row);
}

$(document).ready(function () {
    $("#server-connect").click(function () {
        connectToServer();
    });

    $("#server-disconnect").click(function () {
        closeConnection();
    });

    $("#feed-stop").click(function () {
        $.get('/feed/off', function(data) {
          $('#feed').html(data);
      });
    });

    $("#feed-start").click(function () {
        $.get('/feed/on', function(data) {
          $('#feed').html(data);
      });
    });

    $("#feed-status").click(function () {
        $.get('/feed/status', function(data) {
          $('#feed').html(data);
      });
    });



});


