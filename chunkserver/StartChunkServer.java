package chunkserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import transport.TCPServerThread;

// Wrapper Function for creating ChunkServer and Starting its Components 
public class StartChunkServer {
	public static void main(String[] args) {

		if (args.length != 1) {
			System.err.println("Usage: ChunkServer [PORT]");
			System.exit(-1);
		}

		try {
			// Creating Chunk Server instance
			String host = InetAddress.getLocalHost().getHostName();
			int port = Integer.parseInt(args[0]);
			ChunkServer chunkServer = new ChunkServer(host, port);
			System.out.println("Chunk Server started on " + chunkServer.getNickname());

			// Starting the Request Handler
			TCPServerThread serverThread = new TCPServerThread(chunkServer);
			serverThread.start();

			// Starting the Heartbeat thread
			ChunkServerHeartBeat hb = new ChunkServerHeartBeat(chunkServer);
			hb.start();

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
