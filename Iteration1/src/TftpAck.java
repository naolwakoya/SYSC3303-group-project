
public class TftpAck extends TftpPacket{

	int blockNumber = 0;
	
	public TftpAck(int blockNumber){
		this.blockNumber = blockNumber;
	}
	
	public int getBlockNumber(){
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

}
