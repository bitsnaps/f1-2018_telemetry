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
	public static int counter = 0;

	public static void main(String[] args) {
		Main.run(800, 1000);
	}

	public static void run(long sleepInMillis, int stopCounter) {
		log.info("telemetry is about to run...");
		Random rand = new Random();
		try {
			EchoServer server = new EchoServer();
			server.start();

			EchoClient client = new EchoClient();
			while (server.isAlive()) {
//			while (++counter < 100) {
				String echo = client.sendEcho(String.valueOf( Math.abs(rand.nextInt()) ));
				log.info(echo);
				EchoServer.sleep(sleepInMillis);
//				if (counter == 99){
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
}
