import java.io.ByteArrayOutputStream;

/**
 * Represents the TFTP Error packets
 *
 */
public class TftpError extends TftpPacket {

	//The minimum size of the TFTP error packet
	private static final int MIN_LENGTH = 5;
	
	// The error code value
	int errorCode = 0;
	String errorMessage;

	public TftpError(int errorCode, String erroMessage) {
		this.errorCode = errorCode;
		this.errorMessage = erroMessage;
	}

	/**
	 * Returns the error code of the TFTP packet
	 * @return
	 */
	public int getErrorCode() {
		return this.errorCode;
	}

	/**
	 * Returns the error message of the TFTP packet
	 * @return
	 */
	public String getErrorMessage() {
		return this.errorMessage;
	}

	@Override
	public byte[] generateData() {
		// Create the byte array
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		// Start with 0
		data.write(0);
		// Then the opcode
		data.write(5);
		// Then the error code
		data.write(errorCode >> 8);
		data.write(errorCode);
		// Then the error message
		data.write(errorMessage.getBytes(), 0, errorMessage.getBytes().length);
		// Then it end with 0
		data.write(0);
		return data.toByteArray();
	}

	@Override
	public boolean validateFormat(byte[] data, int packetLength) {
		if (data == null)
			return false;
		if (data.length < MIN_LENGTH || data.length > 516)
			return false;
		// Check opcode and error code
		if ((data[0]!=0) && (data[1]!=5) && (data[2]!=0))
			return false;
		// Check that error code has valid value
		if (data[3] < 0 && data[3]>7)
			return false;
		// Check that it ends with 0
		if (data[data.length-1]!=0)
			return false;
		return true;
	}
}
