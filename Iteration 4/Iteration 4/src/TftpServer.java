import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class TftpServer {

	private DatagramSocket serverSocket;
	String fileName;
	public boolean serverOn;
	private boolean isReadRequest;

	public TftpServer() {
		serverOn = true;
		try {
			System.out.println("SERVER IS Instantiated");

			// Create a datagram socket for receiving packets on port 69
			serverSocket = new DatagramSocket(8082);

		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	public void startReceiving() {
		DatagramPacket receivePacket;
		Thread thread;
		TftpRequest req = new TftpRequest();
		;

		System.out.println("Starting server");

		while (serverOn) {

			try {
				// Create a DatagramPacket for receiving packets
				byte data[] = new byte[516];
				receivePacket = new DatagramPacket(data, data.length);
				System.out.println("Waiting...");
				serverSocket.receive(receivePacket);
				// Check if it is a valid tftp request operation
				if (req.validateFormat(receivePacket.getData(), receivePacket.getLength())) {
					// Check if it is a write request
					if (data[1] == 2) {
						System.out.println("Received a write request");
						isReadRequest = false;
					}
					// Check if it is a read request
					if (data[1] == 1) {
						System.out.println("Received a read request");
						isReadRequest = true;
					}
					
					// Start the connection thread
					thread = new Thread(new TftpClientConnectionThread(isReadRequest, receivePacket));
					thread.start();
				}
				else{
					DatagramSocket sendSocket = new DatagramSocket();
					TftpError error = new TftpError(4, "Invalid read or write request");
					sendSocket.send(error.generatePacket(receivePacket.getAddress(), receivePacket.getPort()));
					sendSocket.close();
				}
					
			} catch (SocketTimeoutException e) {
				continue;
			} catch (SocketException e) {
				continue;
			} catch (IOException e) {
				System.out.print("IO Exception: likely:");
				System.out.println("Receive Socket Timed Out.\n" + e);
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	/*
	 * returns the server socket
	 */
	public DatagramSocket getServerSocket() {
		return serverSocket;
	}

	public static void main(String[] args) {

		TftpServer server = new TftpServer();

		Thread controlThread = new Thread(new TftpServerControl(server));

		controlThread.start();

		server.startReceiving();

	}

}
