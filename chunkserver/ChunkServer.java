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
	private FileChunk fchunk;
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

	// Initializes the Free Space to given Configuration value
	public void updateDiskSpace() {
		// TODO
		freeSpace = new File(Config.FILE_DIR).getFreeSpace() / 1000000;
	}

	// Sends Major Heartbeat to the Controller
	public void sendMajorHB() {
		boolean corruptFlag = false;
		fchunk = null;
		try {

			// Checks for Corrupted File Chunk
			// TODO
			for (FileChunk fc : fileChunkList) {
				if (!fc.getChecksum().equals(util.Util.SHA1(Config.FILE_DIR + "/" + fc.getChunkName()))) {
					corruptFlag = true;
					fchunk = fc;
					break;
				}
			}

			TCPSender sender = new TCPSender(Config.CTRL_HOST, Config.CTRL_PORT);
			sender.sendData(Protocol.MAJOR_HB, this, fchunk, corruptFlag);

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
		return prime + ((host == null) ? 0 : host.hashCode());
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

	public void store(FileChunk fc, ArrayList<ChunkServer> chunkServerList, Client client) {
		fileChunkList.add(fc);
		fc.writeChunk();
		fc.writeMeta();
		updateDiskSpace();

		System.out.println("FileChunk: " + fc.getChunkName() + " has been stored on " + this.getNickname());

		chunkServerList.remove(0);

		try {
			if (chunkServerList.size() != 0) {
				TCPSender sender = new TCPSender(chunkServerList.get(0));
				sender.sendData(Protocol.STORE, fc, chunkServerList, client);
			} else {
				TCPSender sender = new TCPSender(client);
				sender.sendData(Protocol.STORE_ACK, this);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void retrieve(FileChunk fc, Client client) {
		for (FileChunk fchunk : fileChunkList) {
			if (fchunk.equals(fc)) {
				try {
					TCPSender sender = new TCPSender(client);
					sender.sendData(Protocol.RETRIEVE_ACK, fchunk);
					break;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public synchronized void fix(ArrayList<ChunkServer> chunkServerList) {
		System.out.println("Valid replications:");
		for (ChunkServer cs : chunkServerList) {
			System.out.println("\t" + cs.getNickname());
		}
		try {
			TCPSender sender = new TCPSender(chunkServerList.get(0));
			sender.sendData(Protocol.FIX, fchunk, this);
			wait();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		for (FileChunk fc : fileChunkList) {
			if (fc.getChunkName().equals(fchunk.getChunkName())) {
				fileChunkList.set(fileChunkList.indexOf(fc), fchunk);
				fchunk.setVersion(fchunk.getVersion() + 1);
				fchunk.setTimestamp(new Timestamp(System.currentTimeMillis()));
				fchunk.writeChunk();
				fchunk.writeMeta();
				updateDiskSpace();
				System.out.println("Chunk fixed: " + fchunk.getChunkName());
				break;
			}
		}
	}

	public void getFileChunk(FileChunk fc, ChunkServer cs) {
		for (FileChunk fchunk : fileChunkList) {
			if (fchunk.getChunkName().equals(fc.getChunkName())) {
				try {
					TCPSender sender = new TCPSender(cs);
					sender.sendData(Protocol.FIX_ACK, fchunk);
					System.out.println("Sending valid chunk to " + cs.getNickname());
					break;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public synchronized void notifyFix(FileChunk fc) {
		this.fchunk = fc;
		notify();
	}

	public void store(FileChunk fc) {
		fileChunkList.add(fc);
		fc.writeChunk();
		fc.writeMeta();
		updateDiskSpace();

		System.out.println("FileChunk: " + fc.getChunkName() + " has been stored on " + this.getNickname());
	}

	public void delete(FileChunk fc) {

		Iterator<FileChunk> iter = fileChunkList.iterator();

		while (iter.hasNext()) {
			FileChunk fchunk = iter.next();
			if (fchunk.getFileName().equals(fc.getFileName())) {
				iter.remove();
				File file = new File(Config.FILE_DIR + "/" + fchunk.getChunkName());
				file.delete();
			}
		}
		updateDiskSpace();
	}

}
