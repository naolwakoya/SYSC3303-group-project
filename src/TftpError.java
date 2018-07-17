import java.io.ByteArrayOutputStream;


public class TftpError extends TftpPacket{

	String erroMessage = "";
	int errorCode = 0;
	
	public TftpError(int errorCode) {
		this.errorCode = errorCode; 
	}
	
	@Override
	public byte[] generateData() {
		return null;
		
	}
}
