import java.io.ByteArrayOutputStream;

public class TftpData extends TftpPacket{
	
	int blockNumber = 0;
	byte[] data = null;
	
	public TftpData(int blockNumber, byte[] data){
		this.blockNumber=blockNumber;
		if(data == null || data.length == 0){
			this.data = new byte[0];
		}
		else {
			this.data = new byte[data.length];
			System.arraycopy(data, 0, this.data, 0, data.length);
		}
	}
	
	@Override
	public byte[] generateData() {
		ByteArrayOutputStream d = new ByteArrayOutputStream();
		d.write(0);
		d.write(3);
		d.write(blockNumber >> 8);
		d.write(blockNumber);
		d.write(data, 0, data.length);
		return d.toByteArray();
	}

}
