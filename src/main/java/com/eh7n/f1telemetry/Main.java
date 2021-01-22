package com.eh7n.f1telemetry;

import com.eh7n.f1telemetry.EchoServer;
import com.eh7n.f1telemetry.EchoClient;
//import org.junit.platform.commons.logging.Logger;
//import org.junit.platform.commons.logging.LoggerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class Main {

	private static final Logger log = LoggerFactory.getLogger(Main.class);

	public static String IP_ADDRESS = "127.0.0.1";
	public static int PORT = 20777;

	public static int counter = 0;

	public static void main(String[] args) {

		int stopCounter = 1000;
		long sleepInMillis = 800;
		boolean serverOnly = false;

		if (args.length > 0){
			sleepInMillis = Integer.valueOf(args[0]);
			stopCounter = Integer.valueOf(args[1]);
			// run only the server (true/false)
			if (args.length > 2){
				serverOnly = Boolean.valueOf(args[2]);
			}
			// IP Address
			if (args.length > 3){
				IP_ADDRESS = args[3];
			}
			// PORT
			if (args.length > 4){
				PORT = Integer.valueOf(args[4]);
			}
		}
		if (serverOnly){
			Main.serverOnly(sleepInMillis, stopCounter);
		} else {
			Main.run(sleepInMillis, stopCounter);
		}
	}

	public static void run(long sleepInMillis, int stopCounter) {
		log.info("telemetry is about to run on "+ IP_ADDRESS +":"+ PORT);

		Random rand = new Random();
		try {
			EchoServer server = new EchoServer();
			server.start();

			EchoClient client = new EchoClient();
			while (server.isAlive()) {
				String value = String.valueOf( Math.abs(rand.nextInt()) );
				// log.info("Server is live with value: "+value+"\nCounter: "+counter);

				String echo = client.sendEcho(value);
				log.info(echo);

				EchoServer.sleep(sleepInMillis);

				if (++counter >= stopCounter){
					client.sendEcho("end");
					client.close();
					System.exit(0);
				}
			}

//			client.sendEcho("end");
//			client.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void serverOnly(long sleepInMillis, int stopCounter) {
		log.info("telemetry server is about to run on port: "+ PORT);

		try {
			EchoServer server = new EchoServer();
			server.start();

			while (server.isAlive()) {

				log.info(String.valueOf( Math.abs(new Random().nextInt()) ));

				EchoServer.sleep(sleepInMillis);

				if (++counter >= stopCounter){
					System.exit(0);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
