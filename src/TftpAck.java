
public class TftpAck extends TftpPacket {

	int blockNumber = 0;
	//The length of the TFTP ACK packet
	private static final int PACKET_LENGTH = 4;

	public TftpAck(int blockNumber) {
		this.blockNumber = blockNumber;
	}
	
	public TftpAck(){};

	/**
	 * Returns the block number of the tftp packet
	 * @return
	 */
	public int getBlockNumber() {
		return blockNumber;
	}
	
	@Override
	public byte[] generateData() {
		byte[] data = new byte[4];
		data[0] = 0;
		data[1] = (byte) 4;
		data[2] = (byte) (blockNumber >> 8);
		data[3] = (byte) (blockNumber);
		return data;
	}

	@Override
	public boolean validateFormat(byte[] data, int packetLength) {
		if (data==null || packetLength < PACKET_LENGTH || packetLength != PACKET_LENGTH)
			return false;
		if ((data[0]!=0) && (data[1] != 4))
			return false;
		int block = ((data[2] << 8) & 0xFF00)
				| (data[3] & 0xFF);
		if (block < 0 && block > 0xFFFF)
			return false;
		return true;
	}

}
