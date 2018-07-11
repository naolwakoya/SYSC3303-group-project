import java.io.ByteArrayOutputStream;

public class TftpRequest extends TftpPacket{
	
	String fileName;
	String type;
	String mode = "ascii";

	public TftpRequest(String fileName, String type){
		this.type = type;
		this.fileName = fileName;
	}
	
	public String getFileName(){
		return fileName;
	}
	
	public String getType(){
		return type;
	}

	@Override
	public byte[] generateData(){
		// Create the byte array
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		// Start with a 0
		data.write(0);
		// Set the action type flag
		if (type == "read")
			data.write(1);
		else
			data.write(2);
		// Write the filename
		data.write(fileName.getBytes(), 0, fileName.getBytes().length);
		// Add another 0
		data.write(0);
		// Write the mode
		data.write(mode.getBytes(),0,mode.getBytes().length);
		// Finish with another 0
		data.write(0);
		
		return data.toByteArray();
		
	}
}
