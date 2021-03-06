package transport;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;

import chunkserver.ChunkServer;
import chunkserver.FileChunk;
import client.Client;
import util.Node;

public class TCPSender implements Serializable {

	private ObjectOutputStream oos;
	private Socket socket;

	// Constructors
	// From Socket
	public TCPSender(Socket socket) throws IOException {
		oos = new ObjectOutputStream(socket.getOutputStream());
		this.socket = socket;
	}

	// From Node
	public TCPSender(Node node) throws IOException {
		Socket socket = new Socket(node.getHost(), node.getPort());
		oos = new ObjectOutputStream(socket.getOutputStream());
		this.socket = socket;
	}

	// From Host and Port
	public TCPSender(String host, int port) throws IOException {
		Socket socket = new Socket(host, port);
		oos = new ObjectOutputStream(socket.getOutputStream());
		this.socket = socket;
	}

	// Send Methods
	public void sendData(int code) throws IOException {
		oos.writeInt(code);
		oos.flush();
		socket.close();
	}

	public void sendData(int code, Node node) throws IOException {
		oos.writeInt(code);
		oos.writeObject(node);
		oos.flush();
		socket.close();
	}

	public void sendData(int code, boolean flag) throws IOException {
		oos.writeInt(code);
		oos.writeBoolean(flag);
		oos.flush();
		socket.close();
	}

	public void sendData(int code, ArrayList<ChunkServer> chunkServerList) throws IOException {
		oos.writeInt(code);
		oos.writeObject(chunkServerList);
		oos.flush();
		socket.close();
	}

	public void sendData(int code, FileChunk fc, ArrayList<ChunkServer> chunkServerList, Client client)
			throws IOException {
		oos.writeInt(code);
		oos.writeObject(fc);
		oos.writeObject(chunkServerList);
		oos.writeObject(client);
		oos.flush();
		socket.close();
	}

	public void sendData(int code, FileChunk fc, Node node) throws IOException {
		oos.writeInt(code);
		oos.writeObject(fc);
		oos.writeObject(node);
		oos.flush();
		socket.close();
	}

	public void sendData(int code, FileChunk fc) throws IOException {
		oos.writeInt(code);
		oos.writeObject(fc);
		oos.flush();
		socket.close();
	}

	public void sendData(int code, ChunkServer cs, FileChunk fc, boolean flag) throws IOException {
		oos.writeInt(code);
		oos.writeObject(cs);
		oos.writeObject(fc);
		oos.writeBoolean(flag);
		oos.flush();
		socket.close();
	}
}
