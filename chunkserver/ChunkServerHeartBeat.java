package chunkserver;

import java.sql.Timestamp;

import util.Config;

// Heartbeat Thread for Chunk Server 
public class ChunkServerHeartBeat extends Thread {

	private ChunkServer cs;
	private int count;

	// Constructor
	public ChunkServerHeartBeat(ChunkServer cs) {
		this.cs = cs;
		count = 0;
	}

	@Override
	public void run() {
		while (true) {
			try {
				// Suspends the thread for a short amount of time
				Thread.sleep(Config.MINOR_INTERVAL * 1000);
				count++;

				// Send Major Heartbeat
				if (count % (Config.MAJOR_INTERVAL / Config.MINOR_INTERVAL) == 0) {
					Timestamp timestamp = new Timestamp(System.currentTimeMillis());
					System.out.println(timestamp + " Major heartbeat: " + cs.getNickname());
					cs.sendMajorHB();
				}

				// Send Minor Heartbeat
				else {
					Timestamp timestamp = new Timestamp(System.currentTimeMillis());
					System.out.println(timestamp + " Minor heartbeat: " + cs.getNickname());
					cs.sendMinorHB();
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
