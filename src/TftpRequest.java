import java.io.ByteArrayOutputStream;

public class TftpRequest extends TftpPacket {

	// Assume filename is at least 1 character long
	private static final int MIN_LENGTH = 10;
	private String fileName;
	private String type;
	private String mode = "ascii";

	public TftpRequest(){
		
	}
	
	public TftpRequest(String fileName, String type) throws IllegalArgumentException {
		if (type.equals("read") || type.equals("write"))
			this.type = type;
		else {
			throw new IllegalArgumentException("invalid read or write request");
		}
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}

	public String getType() {
		return type;
	}

	@Override
	public byte[] generateData() {
		// Create the byte array
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		// Start with a 0
		data.write(0);
		// Set the action type flag
		if (type.equals("read"))
			data.write(1);
		else
			data.write(2);
		// Write the filename
		data.write(fileName.getBytes(), 0, fileName.getBytes().length);
		// Add another 0
		data.write(0);
		// Write the mode
		data.write(mode.getBytes(), 0, mode.getBytes().length);
		// Finish with another 0
		data.write(0);

		return data.toByteArray();

	}

	@Override
	public boolean validFormat(byte[] data, int packetLength) {
		if (data == null || data.length < packetLength || packetLength < MIN_LENGTH)
			return false;
		//Check that the first byte is 0
		if (data[0] != 0) 
			return false;
		//Check that the second byte is 1 or 2
		if ((data[1] != 1) && (data[1] != 2))
			return false;
	
		int x = 1;
		// Parse the filename
		StringBuilder sb = new StringBuilder();
		while (data[++x] != 0 && x < packetLength) {
			sb.append((char) data[x]);
		}
		// Check if 0 after the filename
		if (data[x] != 0)
			return false;
		//Parse the mode
		StringBuilder sb2 = new StringBuilder();
		while(data[++x] !=0 && x < packetLength){
			sb2.append((char)data[x]);
		}
		//Check that the mode is correct
		if(!(sb2.toString().toLowerCase().equals("ascii")) && !(sb2.toString().toLowerCase().equals("octet")))
			return false;
		//Check that the packet ends with a 0
		if (data[packetLength-1]!=0)
			return false;
		return true;
	}
}
