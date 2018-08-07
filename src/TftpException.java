/**
 * Provides exceptions for the ERROR packets during file transfer
 */
public class TftpException extends Exception{

	private static final long serialVersionUID = -8428963099751529598L;
	
	public TftpException(String message) {
		super(message);
	}

}
