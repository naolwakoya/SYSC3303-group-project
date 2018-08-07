import java.net.*;

/**
 * The base class for the TFTP packet
 */
public abstract class TftpPacket {

	/**
	 * Generates the packet data
	 * @return
	 */
	public abstract byte[] generateData();

	/**
	 * Creates a DatagramPacket from the TFTP packet data
	 * @param addr
	 * @param port
	 * @return
	 */
	public DatagramPacket generatePacket(InetAddress addr, int port) {
		byte data[] = this.generateData();
		return new DatagramPacket(data, data.length, addr, port);
	}
	
	/**
	 * Validates the format of the TFTP packet
	 * @param data
	 * @param packetLength
	 * @return
	 */
	public abstract boolean validateFormat(byte[] data, int packetLength);

}
