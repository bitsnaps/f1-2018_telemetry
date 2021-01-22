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
	public static final int PORT = 20777;
	public static int counter = 0;

	public static void main(String[] args) {
		int stopCounter = 1000;
		long sleepInMillis = 800;
		if (args.length > 0){
			sleepInMillis = Integer.valueOf(args[0]);
			stopCounter = Integer.valueOf(args[1]);
		}
		Main.run(sleepInMillis, stopCounter);
	}

	public static void run(long sleepInMillis, int stopCounter) {
		log.info("telemetry is about to run on port: "+ PORT);

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
					log.info("Server is about to stop at value: "+value+"\nCounter: "+counter);
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
}
