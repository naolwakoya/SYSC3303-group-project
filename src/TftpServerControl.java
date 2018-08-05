
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
		server.incThreadCount();
		String order;
		while (server.serverOn) {
			System.out.println("Welcome to the Tftp Server");
			System.out.println("Type quit to close the Server");
			order = input.next();

			if (order.equalsIgnoreCase(QUIT)) {
				server.getServerSocket().close();
				server.serverOn = false;

			}
		}
		server.decThreadCount();
	}
}
