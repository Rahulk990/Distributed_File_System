package util;

public class Config {

	// Networking Details of Controller [Known to Everyone]
	public static String CTRL_HOST = "venus";
	public static int CTRL_PORT = 8085;

	// Details about File Chunks and Default File Directory
	public static String FILE_DIR = "/tmp";
	public static int CHUNK_SIZE = 64000;

	// Details for the Heartbeat Mechanism
	public static int MAJOR_INTERVAL = 30;
	public static int MINOR_INTERVAL = 5;
	public static int CTRL_INTERVAL = 15;
}
