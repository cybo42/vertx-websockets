import org.vertx.groovy.core.http.RouteMatcher
import org.vertx.groovy.core.eventbus.EventBus

def eb = vertx.eventBus

def stocks = [[name: 'AAPL', price: 600]
            , [name: 'FB', price: 20]
            , [name: 'YHOO', price: 72]
            , [name: 'IBM', price: 123]]

def routeMatcher = new RouteMatcher()
def server = vertx.createHttpServer()
def timerID

def config = ["prefix": "/eventbus"]

routeMatcher.get("/") { req ->
    req.response.sendFile("static/home.html")
}

routeMatcher.get("/feed/status") { req ->
  if(timerID){
    req.response.end("feed started id =  $timerID")

  }else{
    req.response.end("Feed Stopped")
  }
}

routeMatcher.get("/feed/on") { req ->
  if(timerID){
    println "Feed already started ID: $timerID"
    req.response.end("Feed already started ID =  $timerID")

  }else{
    timerID = startStockFeed(eb, stocks, 1000L)
    def m = "Feed ID $timerID started".toString()
    req.response.end(m)
    eb.publish("feed.status", [feedId: timerID, status: 'started', message: m, ts: new Date().format('yyyy/MM/dd hh:mm:ss')])
  }
}

routeMatcher.get("/feed/off") { req ->
	if(timerID){
      vertx.cancelTimer(timerID)
      def m = "Feed ID $timerID stopped".toString()
      req.response.end()
      eb.publish("feed.status", [feedId: timerID, status: 'stopped', message: m, ts: new Date().format('yyyy/MM/dd hh:mm:ss')])

      timerID = null
	}else{
      req.response.end("No Feed running")

	}
}


// Maps all requests to /static to files under static directory
routeMatcher.getWithRegEx("^\\/static\\/.*") { req ->
	println "Request for ${req.path} sending file ${req.path.substring(1)}"
    req.response.sendFile(req.path.substring(1))
}

server.requestHandler(routeMatcher.asClosure())

timerID = startStockFeed(eb, stocks, 1000L)
println "Started Period timer $timerID"

eb.registerHandler("some-address") {message -> 
	println "I received a message ${message.body}"
	message.reply new org.vertx.java.core.json.JsonObject([size: message.body.message.size(), status: 'success', count: 0])
}

// Restricts inbound channels to only some-address
//vertx.createSockJSServer(server).bridge(config, [[address: 'some-address']], [[:]])
// Allows all inbound channels
vertx.createSockJSServer(server).bridge(config, [[:]], [[:]])
server.listen(8080)

def startStockFeed(EventBus eb, def stocks, long interval) {
  def timerID = vertx.setPeriodic(interval) {
    stocks.each { st ->

      def last = st.price
      def newPrice = getNewPrice(st)
      st.price = newPrice

      eb.publish("price.up", [name: st.name, price: st.price, last: last, ts: new Date().format('yyyy/MM/dd hh:mm:ss')])

    }
  }

  return timerID
}

def getNewPrice(st) {
  Random rand = new Random()
  def change = rand.nextInt(10)
  def up = rand.nextBoolean()
  def current = st.price
  def newPrice = (up)? current + change: current - change;

  return newPrice
}





