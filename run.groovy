// @Grab("org.slf4j:slf4j-api:1.7.25")
// @Grab("org.slf4j:slf4j-simple:1.7.25")
//or
// @Grab("org.slf4j:jul-to-slf4j:1.7.25")
// @Grab("org.slf4j:log4j-over-slf4j:1.7.25")
// @Grab('ch.qos.logback:logback-classic:1.2.3')

// @Grab('com.fasterxml.jackson.core:jackson-databind:2.9.10.5')
// @Grab('com.fasterxml.jackson.core:jackson-core:2.9.7')
// @Grab('com.fasterxml.jackson.core:jackson-annotations:2.9.7')

import com.eh7n.f1telemetry.Main
// import groovy.util.logging.Slf4j
// import org.slf4j.*


// def log = LoggerFactory.getLogger(this.class)

// def bindAddress = '0.0.0.0'
// def port = 20777
println("Telemetry UDP Server is gonna starting...")
// Main.run(bindAddress, port)
Main.run(800, 1000)
/*
log.info("F1 2018 - Telemetry UDP Server")
log.info("Listening on ${bindAddress}:${port}...")

Main.create()
  .bindTo(bindAddress)
  .onPort(port)
  .consumeWith({ def p ->
      log.trace(p.toJSON())
    })
  .start()
*/
