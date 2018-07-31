import java.net.*;

public abstract class TftpPacket {

	public abstract byte[] generateData();

	public DatagramPacket generatePacket(InetAddress addr, int port) {
		byte data[] = this.generateData();
		return new DatagramPacket(data, data.length, addr, port);
	}
	
	public abstract boolean validateFormat(byte[] data, int packetLength);

}
