import org.vertx.groovy.core.streams.Pump
import org.vertx.groovy.core.http.RouteMatcher

def routeMatcher = new RouteMatcher()
def server = vertx.createHttpServer()


def config = ["prefix": "/eventbus"]

routeMatcher.get("/") { req ->
    req.response.sendFile("static/home.html")
}

routeMatcher.get("/vertxbus.js") { req ->
    req.response.sendFile("vertxbus.js")
}

// Maps all requests to /static to files under static directory
routeMatcher.getWithRegEx("^\\/static\\/.*") { req ->
	println "Request for ${req.path} sending file ${req.path.substring(1)}"
    req.response.sendFile(req.path.substring(1))
}

server.requestHandler(routeMatcher.asClosure())

def eb = vertx.eventBus

Random rand = new Random()  

def stocks = [[name: 'AAPL', price: 600]
            , [name: 'FB', price: 20]
            , [name: 'YHOO', price: 72]
            , [name: 'IBM', price: 123]]

def timerID = vertx.setPeriodic(1000l) {
    stocks.each{ st ->

	    def change = rand.nextInt(10)
	    def up = rand.nextBoolean()
	    def last = st.price
	    def prev = last
	    if(up){
	    	last = last + change;
	    }else{
	    	last = last - change;

	    }

	    st.price = last

	    eb.publish("price.up", [name: st.name, price: last, last: prev, ts: new Date().format('yyyy/MM/dd hh:mm:ss')])

    }
}

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

