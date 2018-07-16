package test;
import java.net.InetAddress;

import com.sun.xml.internal.ws.api.message.Packet;

public class ErrorChecker {
	private InetAddress mPacketOriginatingAddress; // Temporary name.
	private int mPacketOriginatingPort; // Temporary name.
	private int mExpectedBlockNumber;
	
	public ErrorChecker(Packet packet) {
		mPacketOriginatingAddress = packet.getPacket().getAddress();
		mPacketOriginatingPort = packet.getPacket().getPort();
		mExpectedBlockNumber = 0;

	}

}
