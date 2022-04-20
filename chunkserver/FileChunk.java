package chunkserver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Arrays;

// Implements the File Chunk
public class FileChunk implements Serializable {

	private String chunkName; // Name of the Chunk
	private byte[] content; // Content as Byte Stream
	private int frag; // Chunk or Fragment Number
	private int version; // Version Number
	private Timestamp timestamp; // Last Updation Time
	private String fileName; // Original File Name
	private String checksum; // Checksum of the chunk

	// Constructor to initialize the File Chunk
	public FileChunk(String fileName, int frag, byte[] content) {
		chunkName = fileName + "_chunk" + frag;
		this.frag = frag;
		this.content = content;
		this.fileName = fileName;
		version = 0;
		timestamp = new Timestamp(System.currentTimeMillis());
	}

	// Constructor to initialize the Empty File Chunk
	public FileChunk(String fileName) {
		this.fileName = fileName;
	}

	// Stores the File Chunk on Disk (as File)
	public void writeChunk(int port) {
		File file = new File(port + "_" + chunkName);

		try {
			if (!file.exists()) {
				file.createNewFile();
			}

			FileOutputStream fos = new FileOutputStream(file);
			fos.write(content);
			fos.flush();
			fos.close();

			// Update Checksum for the Chunk
			checksum = util.Util.SHA1(file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	// Stores the Meta Data about the File Chunk on Disk
	public void writeMeta(int port) {
		File file = new File(port + "_" + chunkName + "_meta");

		try {
			FileOutputStream fos = new FileOutputStream(file);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			bw.write(version + "\n");
			bw.write(frag + "\n");
			bw.write(fileName + "\n");
			bw.write(timestamp + "\n");
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// Returns FileName
	public String getFileName() {
		return fileName;
	}

	// Return Version Number
	public int getVersion() {
		return version;
	}

	// Set Version Number
	public void setVersion(int version) {
		this.version = version;
	}

	// Set Timestamp
	public void setTimestamp(Timestamp timestamp) {
		this.timestamp = timestamp;
	}

	// Returns Checksum
	public String getChecksum() {
		return checksum;
	}

	// Returns Content
	public byte[] getContent() {
		return content;
	}

	// Returns the ChunkName
	public String getChunkName() {
		return chunkName;
	}

	// Returns the Fragment Number
	public int getFrag() {
		return frag;
	}

	// Returns Serialized Chunk as String
	public String toString() {
		String ret = chunkName + "\n" + new String(content) + "\n";
		return ret;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((chunkName == null) ? 0 : chunkName.hashCode());
		result = prime * result + Arrays.hashCode(content);
		result = prime * result + frag;
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

		FileChunk other = (FileChunk) obj;
		if (chunkName == null) {
			if (other.chunkName != null)
				return false;
		} else if (!chunkName.equals(other.chunkName))
			return false;
		if (!Arrays.equals(content, other.content))
			return false;
		if (frag != other.frag)
			return false;

		return true;
	}
}
