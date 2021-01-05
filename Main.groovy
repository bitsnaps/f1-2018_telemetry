

//@Grab('org.slf4j:slf4j-api:1.7.25')
//@Grab('org.slf4j:slf4j-log4j12:1.7.25')
@Grab("org.slf4j:jul-to-slf4j:1.7.25")
@Grab("org.slf4j:log4j-over-slf4j:1.7.25")

@Grab('com.fasterxml.jackson.core:jackson-databind:2.9.10.5')
@Grab('com.fasterxml.jackson.core:jackson-core:2.9.7')
@Grab('com.fasterxml.jackson.core:jackson-annotations:2.9.7')
@Grab('ch.qos.logback:logback-classic:1.2.3')

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.DatagramChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Consumer

import groovy.util.logging.Slf4j
//import org.junit.platform.commons.logging.Logger
//import org.junit.platform.commons.logging.LoggerFactory

//import org.slf4j.Logger
//import org.slf4j.LoggerFactory


/**
 * The base class for the F1 2018 Telemetry app. Starts up a non-blocking I/O
 * UDP server to read packets from the F1 2018 video game and then hands those
 * packets off to a parallel thread for processing based on the lambda function
 * defined. Leverages a fluent API for initialization.
 *
 * Also exposes a main method for starting up a default server
 *
 * @author eh7n
 *
 */
@Slf4j
class Main {

//	private static final Logger log = LoggerFactory.getLogger(Main.class)

	private static final String DEFAULT_BIND_ADDRESS = "0.0.0.0"
	private static final int DEFAULT_PORT = 20777
	private static final int MAX_PACKET_SIZE = 1341

	private String bindAddress
	private int port
	private Consumer<Packet> packetConsumer

	private Main() {
		bindAddress = DEFAULT_BIND_ADDRESS
		port = DEFAULT_PORT
	}

	/**
	 * Create an instance of the UDP server
	 *
	 * @return
	 */
	public static Main create() {
		return new Main()
	}

	/**
	 * Set the bind address
	 *
	 * @param bindAddress
	 * @return the server instance
	 */
	Main bindTo(String bindAddress) {
		this.bindAddress = bindAddress
		return this
	}

	/**
	 * Set the bind port
	 *
	 * @param port
	 * @return the server instance
	 */
	Main onPort(int port) {
		this.port = port
		return this
	}

	/**
	 * Set the consumer via a lambda function
	 *
	 * @param consumer
	 * @return the server instance
	 */
	Main consumeWith(Consumer<Packet> consumer) {
		packetConsumer = consumer
		return this
	}

	/**
	 * Start the F1 2018 Telemetry UDP server
	 *
	 * @throws IOException           if the server fails to start
	 * @throws IllegalStateException if you do not define how the packets should be
	 *                               consumed
	 */
	public void start() throws IOException {

		if (packetConsumer == null) {
			throw new IllegalStateException("You must define how the packets will be consumed.")
		}

		log.info("F1 2018 - Telemetry UDP Server")

		// Create an executor to process the Packets in a separate thread
		// To be honest, this is probably an over-optimization due to the use of NIO,
		// but it was done to provide a simple way of providing back pressure on the
		// incoming UDP packet handling to allow for long-running processing of the
		// Packet object, if required.
		ExecutorService executor = Executors.newSingleThreadExecutor()

		try {
      DatagramChannel channel = DatagramChannel.open()
			channel.socket().bind(new InetSocketAddress(bindAddress, port))
			log.info("Listening on " + bindAddress + ":" + port + "...")
			ByteBuffer buf = ByteBuffer.allocate(MAX_PACKET_SIZE)
			buf.order(ByteOrder.LITTLE_ENDIAN)
			while (true) {
				channel.receive(buf)
				final Packet packet = PacketDeserializer.read(buf.array())
				executor.submit({
					packetConsumer.accept(packet)
				})
				buf.clear()
			}
		} finally {
			executor.shutdown()
		}
	}

	/**
	 * Main class in case you want to run the server independently. Uses defaults
	 * for bind address and port, and just logs the incoming packets as a JSON
	 * object to the location defined in the logback config
	 *
	 * @param args
	 * @throws IOException
	 */
	static void main(String[] args) throws IOException {
		println('ok')
		/*Main.create()
							.bindTo("0.0.0.0")
							.onPort(20777)
							.consumeWith{ def p ->
									log.trace(p.toJSON())
								}
							.start()*/
	}
}