package controller;

import java.sql.Timestamp;

import util.Config;

// Heartbeat Thread for Controller
public class ControllerHeartBeat extends Thread {

	private Controller ctrl;

	// Constructor
	public ControllerHeartBeat(Controller ctrl) {
		this.ctrl = ctrl;
	}

	@Override
	public void run() {
		while (true) {
			try {
				// Sleeps for short time interval
				Thread.sleep(Config.CTRL_INTERVAL * 1000);

				// Sends Controller Heartbeat
				Timestamp timestamp = new Timestamp(System.currentTimeMillis());
				System.out.println("\n" + timestamp + " Controller heartbeat: " + ctrl.getNickname());
				ctrl.sendHB();

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
