package transport;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;

import chunkserver.ChunkServer;
import chunkserver.FileChunk;
import client.Client;
import controller.Controller;
import util.Node;

// A new Thread to handle individual Client request at Node
public class TCPReceiverThread extends Thread {

	private Node node;
	private Socket socket;
	private ObjectInputStream ois;

	// Constructor
	public TCPReceiverThread(Node node, Socket socket) throws IOException {
		this.node = node;
		this.socket = socket;
		ois = new ObjectInputStream(this.socket.getInputStream());
	}

	@Override
	public void run() {
		try {
			// Reads the operation code of the received packet
			int packetType = ois.readInt();

			switch (packetType) {

			// [Controller] Register the Chunk Server
			case (Protocol.REGISTER):
				ChunkServer chunkServer = (ChunkServer) ois.readObject();
				((Controller) node).register(chunkServer);
				break;

			// [Chunk Server] Acknowledgement of Registration
			case (Protocol.REGISTER_ACK):
				boolean flag = ois.readBoolean();
				((ChunkServer) node).notify(flag);
				break;

			// [Controller] Handles Major Heartbeat from ChunkServer
			// TODO
			case (Protocol.MAJOR_HB):
				chunkServer = (ChunkServer) ois.readObject();
				FileChunk fc = (FileChunk) ois.readObject();
				flag = ois.readBoolean();
				((Controller) node).processMajorHB(chunkServer, fc, flag);
				break;

			// [Controller] Handles Minor Heartbeat from ChunkServer
			case (Protocol.MINOR_HB):
				chunkServer = (ChunkServer) ois.readObject();
				((Controller) node).processMinorHB(chunkServer);
				break;

			// [Chunk Server] Echo the Heartbeat received from Controller
			case (Protocol.CTRL_HB):
				System.out.println("Received HB from Controller");
				break;

			// [Controller] Accepts Chunk Server request from Client
			case (Protocol.SERVER3):
				Client client = (Client) ois.readObject();
				((Controller) node).getServer3(client);
				break;

			// [Client] Requested Chunk Server List from Controller
			case (Protocol.SERVER3_ACK):
				ArrayList<ChunkServer> chunkServerList = (ArrayList<ChunkServer>) ois.readObject();
				((Client) node).notifyServer3(chunkServerList);
				break;

			// [Chunk Server] Store the given File Chunk
			case (Protocol.STORE):
				fc = (FileChunk) ois.readObject();
				chunkServerList = (ArrayList<ChunkServer>) ois.readObject();
				client = (Client) ois.readObject();
				((ChunkServer) node).store(fc, chunkServerList, client);
				break;

			// [Client] Acknowledgement of Successful Storing of File Chunk
			case (Protocol.STORE_ACK):
				((Client) node).notifyStore();
				break;

			// [Controller] Request for Chunk Server that stores given File Chunk
			case (Protocol.CTRL_RETRIEVE):
				fc = (FileChunk) ois.readObject();
				client = (Client) ois.readObject();
				((Controller) node).getChunkServer(fc, client);
				break;

			// [Client] Requested Chunk Server from Controller
			case (Protocol.CTRL_RETRIEVE_ACK):
				chunkServer = (ChunkServer) ois.readObject();
				((Client) node).notifyRetrieveCTRL(chunkServer);
				break;

			// [Chunk Server] Retrieve given File Chunk
			case (Protocol.RETRIEVE):
				fc = (FileChunk) ois.readObject();
				client = (Client) ois.readObject();
				((ChunkServer) node).retrieve(fc, client);
				break;

			// [Client] Requested File Chunk from Chunk Server
			case (Protocol.RETRIEVE_ACK):
				fc = (FileChunk) ois.readObject();
				((Client) node).notifyRetrieve(fc);
				break;

			case (Protocol.CTRL_FIX):
				chunkServerList = (ArrayList<ChunkServer>) ois.readObject();
				((ChunkServer) node).fix(chunkServerList);
				break;

			case (Protocol.FIX):
				fc = (FileChunk) ois.readObject();
				chunkServer = (ChunkServer) ois.readObject();
				((ChunkServer) node).getFileChunk(fc, chunkServer);
				break;

			case (Protocol.FIX_ACK):
				fc = (FileChunk) ois.readObject();
				((ChunkServer) node).notifyFix(fc);
				break;

			case (Protocol.REDIS):
				fc = (FileChunk) ois.readObject();
				((ChunkServer) node).store(fc);
				break;

			// [Controller] Request to Delete Given File from File System
			case (Protocol.REQ_DEL):
				fc = (FileChunk) ois.readObject();
				client = (Client) ois.readObject();
				((Controller) node).getDelServer(fc, client);
				break;

			// [Client] Request Chunk Server List from Controller
			case (Protocol.REQ_DEL_ACK):
				chunkServerList = (ArrayList<ChunkServer>) ois.readObject();
				((Client) node).notifyDelServer(chunkServerList);
				break;

			// [Chunk Server] Request to Delete given File Chunk
			case (Protocol.DEL):
				fc = (FileChunk) ois.readObject();
				((ChunkServer) node).delete(fc);
				break;

			default:
				System.err.println("Unknown Packet Type" + packetType);
			}

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
