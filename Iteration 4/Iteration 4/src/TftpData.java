import java.io.ByteArrayOutputStream;

public class TftpData extends TftpPacket {

	static final int MAX_SIZE = 512;
	static final int MIN_SIZE = 4;
	int blockNumber = 0;
	byte[] data = null;
	int dataLength;

	public TftpData(int blockNumber, byte[] data, int dataLength) {
		this.blockNumber = blockNumber;
		if (data == null || dataLength == 0) {
			this.data = new byte[0];
			this.dataLength = dataLength;
		} else {
			this.data = new byte[dataLength];
			this.dataLength = dataLength;
			System.arraycopy(data, 0, this.data, 0, dataLength);
		}
	}
	
	public TftpData(){};

	@Override
	public byte[] generateData() {
		ByteArrayOutputStream d = new ByteArrayOutputStream();
		d.write(0);
		d.write(3);
		d.write(blockNumber >> 8);
		d.write(blockNumber);
		d.write(data, 0, dataLength);
		return d.toByteArray();
	}

	@Override
	public boolean validateFormat(byte[] data, int packetLength) {
		if (data == null)
			return false;
		if (packetLength > data.length || packetLength < MIN_SIZE || packetLength > 516){
			return false;

		}
		// Check the opcode
		if ((data[0] != 0) && (data[1] != 3))
			return false;
		//Extract the block number
		int block = ((data[2] << 8) & 0xFF00)
				| (data[3] & 0xFF);
		if (block < 0 && block > 0xFFFF)
			return false;
		return true;
	}

}
