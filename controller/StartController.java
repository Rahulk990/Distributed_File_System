package controller;

import java.io.IOException;
import transport.TCPServerThread;
import util.Config;

// Wrapper Function for creating Controller Instance and Starting its Components 
public class StartController {
	public static void main(String[] args) {

		// Creating Controller Instance
		Controller ctrl = new Controller(Config.CTRL_HOST, Config.CTRL_PORT);

		try {
			// Starting the Request Handler
			TCPServerThread serverThread = new TCPServerThread(ctrl);
			serverThread.start();
			System.out.println("Controller started on " + ctrl.getNickname());

			// Wait for first Major Heartbeat
			Thread.sleep(Config.MINOR_INTERVAL * 1000);
			ctrl.requestMajorHB();

			// Starting the Heartbeat thread
			ControllerHeartBeat hb = new ControllerHeartBeat(ctrl);
			hb.start();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
