import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class TftpDelayThread implements Runnable{
	private DatagramPacket packet;
	private DatagramSocket socket;
	private int delay;
	
	public TftpDelayThread(int delay, DatagramPacket packet, DatagramSocket socket){
		this.delay  = delay;
		this.socket = socket;
		this.packet = packet;
	}
	
	@Override
	public void run(){

		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (!socket.isClosed()) {
		try {
			socket.send(this.packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
		}
	}
}
