import java.io.ByteArrayOutputStream;

public class TftpData extends TftpPacket{
	
	static int MAX_SIZE = 512;
	int blockNumber = 0;
	byte[] data = null;
	int dataLength;
	
	public TftpData(int blockNumber, byte[] data, int dataLength){
		this.blockNumber=blockNumber;
		if(data == null || dataLength == 0){
			this.data = new byte[0];
			this.dataLength = dataLength;
		}
		else {
			this.data = new byte[dataLength];
			this.dataLength = dataLength;
			System.arraycopy(data, 0, this.data, 0, dataLength);
		}
	}
	
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

}
