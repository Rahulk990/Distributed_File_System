package client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

import transport.TCPServerThread;

// Wrapper Function for creating ChunkServer and Starting its Components 
public class StartClient {
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: ChunkServer [PORT]");
			System.exit(-1);
		}

		Client client = null;
		try {

			// Creating Chunk Server instance
			String host = InetAddress.getLocalHost().getHostName();
			int port = Integer.parseInt(args[0]);
			client = new Client(host, port);

			// Starting the Request Handler
			TCPServerThread serverThread = new TCPServerThread(client);
			serverThread.start();

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Please enter action name: [store] [retrieve] [update] [q! for exit]");

		Scanner scan = new Scanner(System.in);
		String actionName = scan.next();

		while (true) {
			switch (actionName) {

			case ("store"):
				System.out.println("\nPlease enter file name:");
				String fileName = scan.next();
				client.store(fileName);
				break;

			case ("retrieve"):
				System.out.println("\nPlease enter file name:");
				fileName = scan.next();
				client.retrieve(fileName);
				break;

			case ("update"):
				System.out.println("\nPlease enter file name:");
				fileName = scan.next();
				client.update(fileName);
				break;

			case ("q!"):
				System.out.println("\nBye!");
				scan.close();
				System.exit(0);

			default:
				System.err.println("\nUnknown Action");
			}

			System.out.println("\nPlease enter action name: [store] [retrieve] [update] [q! for exit]");
			actionName = scan.next();
		}

	}
}
