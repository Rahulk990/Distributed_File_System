package client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import chunkserver.ChunkServer;
import chunkserver.FileChunk;
import transport.Protocol;
import transport.TCPSender;
import util.Config;
import util.Node;

// Implements the Client
public class Client extends Node {

	private ChunkServer chunkServer;
	private ArrayList<ChunkServer> chunkServerList;
	private FileChunk fch;

	// Constructor to initialize the Client
	public Client(String host, int port) {
		super(host, port);
		chunkServerList = new ArrayList<>();
	}

	// Reads teh given File and Stores on the File System
	public synchronized void store(String fileName) {

		try {

			// Split File into Chunks
			ArrayList<FileChunk> fileChunkList = splitFile(fileName);
			for (FileChunk fc : fileChunkList) {

				// Get 3 random Chunk Servers from Controller
				TCPSender sender = new TCPSender(Config.CTRL_HOST, Config.CTRL_PORT);
				sender.sendData(Protocol.SERVER3, this);
				wait();

				// Send to First Chunk Server [They will Propagate automatically]
				sender = new TCPSender(chunkServerList.get(0));
				sender.sendData(Protocol.STORE, fc, chunkServerList, this);
				wait();

				System.out.println(fc.getChunkName() + " has been stored");
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// Awakes the Client on Acknowledgement from Controller
	public synchronized void notifyServer3(ArrayList<ChunkServer> chunkServerList) {
		this.chunkServerList = chunkServerList;
		notify();
	}

	// Awakes the Client on Acknowledgement from Chunk Server
	public synchronized void notifyStore() {
		notify();
	}

	// Retrieves the given File from the File System
	public synchronized void retrieve(String fileName) {

		try {
			ArrayList<FileChunk> fileChunkList = splitFile(fileName);

			ArrayList<FileChunk> chunks = new ArrayList<>();
			for (FileChunk fc : fileChunkList) {

				// Get the Chunk Server that store the File Chunk from Controller
				TCPSender sender = new TCPSender(Config.CTRL_HOST, Config.CTRL_PORT);
				sender.sendData(Protocol.CTRL_RETRIEVE, fc, this);
				wait();

				// Retrieve the File Chunk from the Chunk Server
				sender = new TCPSender(chunkServer);
				sender.sendData(Protocol.RETRIEVE, fc, this);
				wait();

				System.out.println(fch.getChunkName() + " has been retrieved");
				chunks.add(fch);
			}

			// Merge all the File Chunks and store on Disk
			chunks.sort(Comparator.comparing(FileChunk::getFrag));
			mergeFile(chunks, "Retrieved_" + fileName);
			System.out.println(fileName + " has been retrieved");

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// Awakes the Client on Acknowledgement from Controller
	public synchronized void notifyRetrieveCTRL(ChunkServer chunkServer) {
		this.chunkServer = chunkServer;
		notify();
	}

	// Awakes the Client on Acknowledgement from Chunk Server
	public synchronized void notifyRetrieve(FileChunk fc) {
		fch = fc;
		notify();
	}

	// Updates the given File by Deleting and Storing it again
	public synchronized void update(String fileName) {
		delete(fileName);
		store(fileName);
	}

	// Deletes the given File from File System
	public synchronized void delete(String fileName) {
		FileChunk fc = new FileChunk(fileName);

		try {
			// Get all Chunk Servers that store the Chunks of given File
			TCPSender sender = new TCPSender(Config.CTRL_HOST, Config.CTRL_PORT);
			sender.sendData(Protocol.REQ_DEL, fc, this);
			wait();

			// Delete File Chunks from each Chunk Server
			for (ChunkServer cs : chunkServerList) {
				sender = new TCPSender(cs);
				sender.sendData(Protocol.DEL, fc);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// Awakes the Client on Acknowledgement from Controller
	public synchronized void notifyDelServer(ArrayList<ChunkServer> chunkServerList) {
		this.chunkServerList = chunkServerList;
		notify();
	}

	// Reads the given File and Splits it into Chunks
	public static ArrayList<FileChunk> splitFile(String fileName) {

		ArrayList<FileChunk> chunks = new ArrayList<>();
		File inputFile = new File(fileName);
		FileInputStream fis;

		long fileSize = inputFile.length();
		int readLength = Config.CHUNK_SIZE;
		int nChunks = 0, read = 0;
		byte[] byteChunkPart;

		try {
			fis = new FileInputStream(inputFile);

			while (fileSize > 0) {

				if (fileSize < readLength) {
					readLength = (int) fileSize;
				}
				byteChunkPart = new byte[readLength];
				read = fis.read(byteChunkPart, 0, readLength);
				fileSize -= read;

				// Create new File Chunk instance for the given chunk
				FileChunk chunk = new FileChunk(inputFile.getName(), nChunks, byteChunkPart);
				chunks.add(chunk);

				nChunks++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return chunks;
	}

	// Combine all the File Chunks into a single File
	public static void mergeFile(List<FileChunk> chunks, String outputFileName) {
		File file = new File(outputFileName);
		FileOutputStream fos;

		try {
			fos = new FileOutputStream(file);

			// Combining Files into Stream
			for (FileChunk chunk : chunks) {
				fos.write(chunk.getContent());
			}
			fos.flush();
			fos.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
