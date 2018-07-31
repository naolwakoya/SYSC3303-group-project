
import java.util.Scanner;

public class TftpServerControl implements Runnable {

	public static final String QUIT = "QUIT";
	private Scanner input;
	TftpServer server;

	public TftpServerControl(TftpServer server) {
		this.server = server;
		input = new Scanner(System.in);
	}

	@Override
	public void run() {
		String order;
		while (server.serverOn) {
			System.out.println("Write quit to close the Server");
			order = input.next();

			if (order.equalsIgnoreCase(QUIT)) {
				System.out.println("The Server is Shutting Down ");
				server.serverOn = false;
				server.getServerSocket().close();
			}
		}
	}
}
