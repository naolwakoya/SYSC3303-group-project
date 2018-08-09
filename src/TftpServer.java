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
	int threadCount = 0;

	public TftpServer() {
		
		serverOn = true;
		try {
			// Create a datagram socket for receiving packets on port 69
			serverSocket = new DatagramSocket(69);

		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Wait to receive a request packet from a client
	 */
	public void startReceiving() {
		DatagramPacket receivePacket;
		Thread thread;
		TftpRequest req = new TftpRequest();

		while (serverOn) {

			try {
				// Create a DatagramPacket for receiving packets
				byte data[] = new byte[516];
				receivePacket = new DatagramPacket(data, data.length);
				System.out.println("SERVER: Waiting for request...");
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
					thread = new Thread(new TftpClientConnectionThread(this, isReadRequest, receivePacket));
					thread.start();
				} else {
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
		System.out.println("Quitting server... waiting for all threads to finish");
		while (getThreadCount()>0) {
			try {
				wait();
			}
			catch (IllegalMonitorStateException e) {
				
			}
			catch (InterruptedException e) {
				System.out.println("Quit was interrupted. Failed to quit.");
				System.exit(1);
			}
		}
		System.out.println("Successful shutdown");
		System.exit(0);

	}

	synchronized public void incThreadCount() {
		threadCount++;
	}

	synchronized public void decThreadCount() {
		threadCount--;
		if (threadCount <= 0) {
			notifyAll();
		}
	}

	synchronized public int getThreadCount() {
		return threadCount;
	}

	/**
	 * Returns the server socket
	 * @return
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
