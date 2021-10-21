package transport;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import util.Node;

// A new Thread to handle Client requests
public class TCPServerThread extends Thread {

	private int port;
	private Node node;
	private ServerSocket serverSocket;

	// Constructor
	public TCPServerThread(Node node) throws IOException {
		this.node = node;
		port = node.getPort();
		serverSocket = new ServerSocket(port);
	}

	@Override
	public void run() {
		while (true) {
			Socket socket = null;
			try {
				socket = serverSocket.accept();

				// Create a new Thread to handle the incoming connection
				TCPReceiverThread receiverThread = new TCPReceiverThread(node, socket);
				receiverThread.start();

			} catch (IOException ioe) {
				ioe.printStackTrace();
				continue;
			}
		}
	}
}
