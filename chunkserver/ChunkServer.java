package chunkserver;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;

import client.Client;
import transport.Protocol;
import transport.TCPSender;
import util.Config;
import util.Node;

// Implements the Chunk Server
public class ChunkServer extends Node {

	private String host;
	private long freeSpace;
	private FileChunk fChunk;
	private ArrayList<FileChunk> fileChunkList;

	// Constructor to initialize Chunk Server
	public ChunkServer(String host, int port) {
		super(host, port);

		this.host = host;
		updateDiskSpace();
		fileChunkList = new ArrayList<>();
	}

	// Echo the Acknowledgement about Registration with Controller
	public void notify(boolean flag) {
		System.out.println(flag);
	}

	// Updates the available Free space in the Chunk Server
	public void updateDiskSpace() {
		freeSpace = new File(Config.FILE_DIR).getFreeSpace() / 1000000;
	}

	// Sends Major Heartbeat to the Controller
	public void sendMajorHB() {
		boolean corruptFlag = false;
		fChunk = null;
		try {

			// Checks for Corrupted File Chunk
			for (FileChunk fc : fileChunkList) {
				if (!fc.getChecksum().equals(util.Util.SHA1(Config.FILE_DIR + "/" + fc.getChunkName()))) {
					corruptFlag = true;
					fChunk = fc;
					break;
				}
			}

			// Send Heartbeat to Controller
			TCPSender sender = new TCPSender(Config.CTRL_HOST, Config.CTRL_PORT);
			sender.sendData(Protocol.MAJOR_HB, this, fChunk, corruptFlag);

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Sends Minor Heartbeat to the Controller
	public void sendMinorHB() {
		try {
			TCPSender sender = new TCPSender(Config.CTRL_HOST, Config.CTRL_PORT);
			sender.sendData(Protocol.MINOR_HB, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Returns the File Chunk List
	public ArrayList<FileChunk> getFileChunkList() {
		return fileChunkList;
	}

	// Returns the available Free space
	public long getFreeSpace() {
		return freeSpace;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		ChunkServer other = (ChunkServer) obj;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		return true;
	}

	// Stores the File Chunk on Disk
	public void store(FileChunk fc, ArrayList<ChunkServer> chunkServerList, Client client) {
		fileChunkList.add(fc);
		fc.writeChunk();
		fc.writeMeta();
		updateDiskSpace();

		System.out.println("FileChunk: " + fc.getChunkName() + " has been stored on " + this.getNickname());

		try {
			chunkServerList.remove(0);

			// Forwards to the next Chunk Server, if required
			if (chunkServerList.size() != 0) {
				TCPSender sender = new TCPSender(chunkServerList.get(0));
				sender.sendData(Protocol.STORE, fc, chunkServerList, client);
			}

			// else, Sends the Acknowledgement to the Client
			else {
				TCPSender sender = new TCPSender(client);
				sender.sendData(Protocol.STORE_ACK, this);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Retrieve the File Chunk and send to Client
	public void retrieve(FileChunk fc, Client client) {
		for (FileChunk fChunk : fileChunkList) {
			if (fChunk.equals(fc)) {
				try {
					TCPSender sender = new TCPSender(client);
					sender.sendData(Protocol.RETRIEVE_ACK, fChunk);
					break;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// Fix the corrupted File Chunk from another Replica
	public synchronized void fix(ArrayList<ChunkServer> chunkServerList) {
		System.out.println("Valid replications:");
		for (ChunkServer cs : chunkServerList) {
			System.out.println("\t" + cs.getNickname());
		}

		try {
			// Ask for correct File Chunk
			TCPSender sender = new TCPSender(chunkServerList.get(0));
			sender.sendData(Protocol.FIX, fChunk, this);
			wait();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Fix the corrupted File Chunk
		for (FileChunk fc : fileChunkList) {
			if (fc.getChunkName().equals(fChunk.getChunkName())) {
				fileChunkList.set(fileChunkList.indexOf(fc), fChunk);
				fChunk.setVersion(fChunk.getVersion() + 1);
				fChunk.setTimestamp(new Timestamp(System.currentTimeMillis()));
				fChunk.writeChunk();
				fChunk.writeMeta();
				updateDiskSpace();

				System.out.println("Chunk fixed: " + fChunk.getChunkName());
				break;
			}
		}
	}

	// Return the asked File Chunk to given Chunk Server
	public void getFileChunk(FileChunk fc, ChunkServer cs) {
		for (FileChunk fChunk : fileChunkList) {
			if (fChunk.getChunkName().equals(fc.getChunkName())) {
				try {
					// Sending to Chunk Server
					TCPSender sender = new TCPSender(cs);
					sender.sendData(Protocol.FIX_ACK, fChunk);

					System.out.println("Sending valid chunk to " + cs.getNickname());
					break;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// Awakes the Chunk Server on Acknowledgement from other Chunk Server
	public synchronized void notifyFix(FileChunk fc) {
		this.fChunk = fc;
		notify();
	}

	// Stores the given Chunk on Disk
	public void store(FileChunk fc) {
		fileChunkList.add(fc);
		fc.writeChunk();
		fc.writeMeta();
		updateDiskSpace();

		System.out.println("FileChunk: " + fc.getChunkName() + " has been stored on " + this.getNickname());
	}

	// Remove all File Chunks related to given File
	public void delete(FileChunk fc) {
		Iterator<FileChunk> iter = fileChunkList.iterator();
		while (iter.hasNext()) {
			FileChunk fChunk = iter.next();
			if (fChunk.getFileName().equals(fc.getFileName())) {
				iter.remove();

				File file = new File(Config.FILE_DIR + "/" + fChunk.getChunkName());
				file.delete();
			}
		}
		updateDiskSpace();
	}
}
