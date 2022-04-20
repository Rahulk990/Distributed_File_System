package controller;

import java.io.IOException;
import java.net.ConnectException;
import java.sql.Timestamp;
import java.util.ArrayList;

import chunkserver.ChunkServer;
import chunkserver.FileChunk;
import client.Client;
import transport.Protocol;
import transport.TCPSender;
import util.Config;
import util.Node;
import util.Util;

// Implements the Controller
public class Controller extends Node {

	private ArrayList<ChunkServer> chunkList;

	// Constructor to initialize the Controller
	public Controller(String host, int port) {
		super(host, port);
		chunkList = new ArrayList<>();
	}

	// Registers the Chunk Server with the Controller
	public synchronized boolean register(ChunkServer chunkServer) {
		boolean ret = false;
		try {
			// Adds the Chunk Server to ChunkList
			chunkList.add(chunkServer);
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			System.out.println("\n" + timestamp + " ChunkServer joined: " + chunkServer.getNickname());

			// Sends back the Acknowledgement to the Chunk Server
			ret = true;
			TCPSender sender = new TCPSender(chunkServer);
			sender.sendData(Protocol.REGISTER_ACK, ret);

		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}

	// Process Major HeartBeat Message and Initiate Fixes of any Corrupted FileChunk
	public synchronized void processMajorHB(ChunkServer chunkServer, FileChunk fc, boolean flag) {
		ArrayList<ChunkServer> fixServers = new ArrayList<>();

		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		System.out.println("\n" + timestamp);

		// Get all Chunk Servers that contain Corrupted FileChunk
		System.out.println("Available chunk servers:");
		for (ChunkServer cs : chunkList) {
			if (cs.equals(chunkServer)) {
				chunkList.set(chunkList.indexOf(cs), chunkServer);
			}

			System.out.println("\t" + cs.getNickname() + ": " + cs.getusedSpace() + " Bytes");

			ArrayList<FileChunk> chunks = cs.getFileChunkList();
			for (FileChunk chunk : chunks) {
				if (flag == true) {
					if (chunk.getChunkName().equals(fc.getChunkName()) && !cs.equals(chunkServer)) {
						fixServers.add(cs);
					}
				}

				System.out.println("\t\t" + chunk.getChunkName());
			}
		}

		if (flag == true) {
			System.err.println("\nData corruption on " + chunkServer.getNickname() + ": " + fc.getChunkName());

			try {
				// Send list of Servers that contain Correct Chunk back to corrupted chunkServer(doubt)
				TCPSender sender = new TCPSender(chunkServer);
				sender.sendData(Protocol.CTRL_FIX, fixServers);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Process Minor HeartBeat Message and Updates the ChunkList
	public synchronized void processMinorHB(ChunkServer chunkServer) {
		if (chunkList.contains(chunkServer)) {
			for (ChunkServer cs : chunkList) {

				// Overridden equals Function
				if (cs.equals(chunkServer)) {
					chunkList.set(chunkList.indexOf(cs), chunkServer); //(doubt) why replace when cs==chunkserver?
					break;
				}
			}
		} else {
			register(chunkServer);
		}
	}

	// Returns 3 random Chunk Servers to the Client
	public synchronized void getServer3(Client c) {

		ArrayList<ChunkServer> randomList = new ArrayList<>();
		System.out.println(chunkList.size());

		// Find 3 random Chunk Servers
		while (randomList.size() < Config.REPLICAS) {
			int index = Util.getRandInt(0, chunkList.size());
			if (!randomList.contains(chunkList.get(index))) {
				randomList.add(chunkList.get(index));
			}
		}

		try {
			// Send the List back to the Client
			TCPSender sender = new TCPSender(c);
			sender.sendData(Protocol.SERVER3_ACK, randomList);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Returns the Chunk Server that contains the requested FileChunk
	public synchronized void getChunkServer(FileChunk fc, Client c) {
		for (ChunkServer cs : chunkList) {
			if (cs.getFileChunkList().contains(fc)) {
				try {
					// Sending back to Client
					TCPSender sender = new TCPSender(c);
					sender.sendData(Protocol.CTRL_RETRIEVE_ACK, cs);
					break;

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// Send Heartbeat and Handle Terminated Servers
	public synchronized void sendHB() {
		ArrayList<ChunkServer> downServer = new ArrayList<>();
		try {

			// Sending Heartbeat to all Chunk Servers
			for (ChunkServer cs : chunkList) {
				try {
					TCPSender sender = new TCPSender(cs);
					sender.sendData(Protocol.CTRL_HB);
				} catch (ConnectException e) {
					downServer.add(cs);
				}
			}

			// Handling Terminated Servers
			System.out.println();
			for (ChunkServer downCS : downServer) {
				Timestamp timestamp = new Timestamp(System.currentTimeMillis());
				System.out.println(timestamp + " " + downCS.getNickname() + " is down");
				System.out.println("Redistributing files...");

				// Redistribute the File Chunks to First available Chunk Server
				ArrayList<FileChunk> fileChunkList = downCS.getFileChunkList();
				for (FileChunk fc : fileChunkList) {
					for (ChunkServer cs : chunkList) {
						if (!cs.equals(downCS) && !cs.getFileChunkList().contains(fc)) {
							TCPSender sender = new TCPSender(cs);
							sender.sendData(Protocol.REDIS, fc);
							break;
						}
					}
				}

				// Removing the Chunk Server from List of active Chunk Servers
				chunkList.remove(chunkList.indexOf(downCS));
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Request for Major Heartbeat from all Chunk Servers
	public void requestMajorHB() {
		for (ChunkServer cs : chunkList) {
			cs.sendMajorHB();
		}
	}

	// Return all Chunk Servers that store the Chunks of given File
	public void getDelServer(FileChunk fc, Client c) {
		ArrayList<ChunkServer> delServer = new ArrayList<>();

		for (ChunkServer cs : chunkList) {
			for (FileChunk fchunk : cs.getFileChunkList()) {
				if (fchunk.getFileName().equals(fc.getFileName())) {
					delServer.add(cs);
					break;
				}
			}
		}

		try {
			// Sending the list to the Client
			TCPSender sender = new TCPSender(c);
			sender.sendData(Protocol.REQ_DEL_ACK, delServer);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
