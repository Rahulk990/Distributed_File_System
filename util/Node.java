package util;

import java.io.Serializable;

// Serializable allows the objects to be converted to byte streams
// which can then be written to or read from any Input/Output Stream
public class Node implements Serializable {

	// Networking Details about the Node
	private String host;
	private int port;

	// Constructor to initialize the Node
	public Node(String host, int port) {
		this.host = host;
		this.port = port;
	}

	// Returns the node details in form of a string
	public String toString() {
		return "[" + host + ":" + port + "]";
	}

	// Returns the host value of the Node
	public String getHost() {
		return host;
	}

	// Returns the port value of the Node
	public int getPort() {
		return port;
	}

	// Returns the Nickname of the Node
	public String getNickname() {
		return host + "-" + String.valueOf(port);
	}
}
