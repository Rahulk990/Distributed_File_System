package util;

public class Config {

	// Networking Details of Controller [Known to Everyone]
	public static String CTRL_HOST = "DESKTOP-0UP15CT";
	public static int CTRL_PORT = 8085;

	// Details about File Chunks and Default File Directory
	public static int CHUNK_SIZE = 256;

	// Details for the Heartbeat Mechanism
	public static int MAJOR_INTERVAL = 30;
	public static int MINOR_INTERVAL = 5;
	public static int CTRL_INTERVAL = 15;

	public static int REPLICAS = 3;
}
