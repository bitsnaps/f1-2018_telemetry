@Grab("org.slf4j:slf4j-api:1.7.25")
@Grab("org.slf4j:slf4j-simple:1.7.25")
// @Grab("org.slf4j:jul-to-slf4j:1.7.25")
// @Grab("org.slf4j:log4j-over-slf4j:1.7.25")

@Grab('com.fasterxml.jackson.core:jackson-databind:2.9.10.5')
@Grab('com.fasterxml.jackson.core:jackson-core:2.9.7')
@Grab('com.fasterxml.jackson.core:jackson-annotations:2.9.7')
// @Grab('ch.qos.logback:logback-classic:1.2.3')


// @Grab("org.slf4j:slf4j-nop:1.7.25")

// @Grab("log4j:log4j:1.2.17")
// @Grab("org.slf4j:slf4j-api:1.7.25")
// @Grab("org.slf4j:slf4j-simple:1.7.25")

// @Grab("ch.qos.logback:logback-classic:1.2.3")

// @Grab("org.slf4j:jul-to-slf4j:1.7.25")
// @Grab("org.slf4j:log4j-over-slf4j:1.7.25")
// @Grab('org.slf4j:slf4j-log4j12:1.7.25')

import groovy.util.logging.Slf4j

// import org.slf4j.Logger

import com.eh7n.f1telemetry.Main
// import java.util.logging.Logger


// Logger log = Logger.getLogger(this.class.simpleName)
// Logger log = LoggerFactory.getLogger(this.class)

//working attemp (logging from Main):
//import org.slf4j.*

@Slf4j
class Launcher {

  static main(args){
    def bindAddress = '0.0.0.0'
    def port = 20777
    log.info("F1 2018 - Telemetry UDP Server")
    log.info("Listening on ${bindAddress}:${port}...")
    Main.create()
      .bindTo(bindAddress)
      .onPort(port)
      .consumeWith({ def p ->
          log.trace(p.toJSON())
        })
      .start()
  }

}
