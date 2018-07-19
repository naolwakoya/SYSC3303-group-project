import java.io.ByteArrayOutputStream;


public class TftpError extends TftpPacket{

	int errorCode = 0;
	String errorMessage;
	
	public TftpError(int errorCode, String erroMessage) {
		this.errorCode = errorCode; 
		this.errorMessage = erroMessage;
	}
	
	public int getErrorCode() {
		return this.errorCode;
	}
	
	public String getErrorMessage() {
		return this.errorMessage;
	}
	
	@Override
	public byte[] generateData() {
		
		//Create the byte array
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		//Start with 0
		data.write(0);
		//Then the opcode
		data.write(5);
		//Then the error code
		data.write(errorCode);
		//Then the error message
		data.write(errorMessage.getBytes(),0,errorMessage.getBytes().length);
		//Then it end with 0
		data.write(0);
		
		return data.toByteArray();
	}
}
