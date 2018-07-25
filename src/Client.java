import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.SyncFailedException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.Scanner;

public class Client {

	int REQUEST_PORT = 69;
	int sourceTID, destinationTID;

	DatagramSocket sendReceiveSocket;
	static DatagramPacket receivePacket;
	DatagramPacket sendPacket;
	String mode = "octet";
	String filePath = System.getProperty("user.dir") + "/clientFiles/";
	boolean connected = false;
	boolean verbose = true;
	boolean test = false;

	public Client() {
		try {
			sendReceiveSocket = new DatagramSocket();

		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	// Establishes a read or write connection with the server according to the
	// TFTP protocol
	public void establishConnection(String fileName, String request) {
		try {

			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}

		TftpRequest req = new TftpRequest(fileName, request);
		try {
			sendPacket = req.generatePacket(InetAddress.getLocalHost(), REQUEST_PORT);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}

		if (verbose) {
			System.out.println("Sending packet:");
			printPacketInformation(sendPacket);
		}

		// Send the packet via the sendReceiveSocket
		try {
			sendReceiveSocket.send(sendPacket);
			sourceTID = sendReceiveSocket.getPort();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		System.out.println("Connected to server");
		connected = true;
	}

	/*
	 * Writes a file to the server
	 */
	public void writeFile(String fileName) {
		try {
			this.receive();
			destinationTID = receivePacket.getPort();
			String filePath = System.getProperty("user.dir") + "/clientFiles/" + fileName;
			File file = new File(filePath);
			// Check if file exists (error code 1)
			if (!file.exists()) {
				System.out.println("Cannot find the file: " + fileName);
				return;
			}
			// Check for Access violation (error code 2)
			if (!file.canRead()) {
				System.out.println("Cannot read file: " + fileName);
				return;
			}

			FileInputStream inputStream = new FileInputStream(file);

			int blockNumber = 1;
			int nRead = 0;
			byte[] data = new byte[512];
			TftpData dataPacket;

			do {
				nRead = inputStream.read(data);
				if (nRead == -1) {
					nRead = 0;
					data = new byte[0];
				}
				dataPacket = new TftpData(blockNumber, data, nRead);
				sendPacket = dataPacket.generatePacket(receivePacket.getAddress(), destinationTID);

				if (verbose) {
					System.out.println("Sending packet:");
					printPacketInformation(sendPacket);
				}

				try {
					sendReceiveSocket.send(sendPacket);
				} catch (IOException e1) {
					e1.printStackTrace();
					System.exit(1);
				}

				this.receive();
				blockNumber++;

			} while (nRead == 512);

			inputStream.close();

		} catch (FileNotFoundException e) {
			System.out.println("Failed to send " + fileName + ": " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IOException: Failed to send " + fileName + ": " + e.getMessage());
		}

	}

	/*
	 * Reads a file from the server
	 */
	public void readFile(String fileName) {
		String filePath = System.getProperty("user.dir") + "/clientFiles/" + fileName;
		try {
			File file = new File(filePath);
			// Check access violation (error code 2)
			if (file.exists() && !file.canWrite()) {
				System.out.println("Can't overwrite existing file");
				return;
			}

			byte[] fileData;
			FileOutputStream outputStream = new FileOutputStream(filePath);
			int blockNumber = 1;

			do {
				this.receive();
				destinationTID = receivePacket.getPort();

				try {
					fileData = parseData(receivePacket.getData(), receivePacket.getLength());
					outputStream.write(fileData);
					outputStream.getFD().sync();
				} catch (SyncFailedException e) {
					file.delete();
					outputStream.close();
					TftpError error = new TftpError(3, "Disk full or allocation exceeded");
					sendReceiveSocket.send(error.generatePacket(receivePacket.getAddress(), destinationTID));
					return;
				}
				// Send acknowledgement packet
				TftpAck ack = new TftpAck(blockNumber);
				sendPacket = ack.generatePacket(receivePacket.getAddress(), destinationTID);
				blockNumber++;

				if (verbose) {
					System.out.println("Sending packet:");
					printPacketInformation(sendPacket);
				}

				try {
					sendReceiveSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			} while (!(fileData.length < 512));
			outputStream.close();
			
		} catch (FileNotFoundException e1) {
			System.out.println("Failed to send " + fileName + ": " + e1.getMessage());
			return;
		} catch (IOException e) {
			System.out.println("IOException: Failed to send " + fileName + ": " + e.getMessage());
			new File(filePath).delete();
			return;
		}
	}

	/*
	 * Waits to receive a packet from the sendReceiveSocket
	 */
	public DatagramPacket receive() {
		// Create a DatagramPacket for receiving packets
		byte receive[] = new byte[1024];
		receivePacket = new DatagramPacket(receive, receive.length);

		try {
			// Block until a datagram is received via sendReceiveSocket.
			sendReceiveSocket.receive(receivePacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		if (verbose) {
			System.out.println("Received packet:");
			printPacketInformation(receivePacket);
		}
		return receivePacket;
	}

	/*
	 * Processes the received Datagram and Prints the packet information into
	 * the console
	 */
	public void printPacketInformation(DatagramPacket packet) {
		System.out.println("Host: " + packet.getAddress());
		System.out.println("Host port: " + packet.getPort());
		System.out.println("Packet length: " + packet.getLength());
		System.out.println("Containing: " + packet.getData());
		String packetString = new String(packet.getData(), 0, packet.getLength());
		System.out.println("String form: " + packetString + "\n");
	}

	public boolean isConnected() {
		if (connected == true)
			return true;
		else
			return false;
	}

	/*
	 * returns true if in verbose mode and false if in quiet mode
	 */
	public boolean getMode() {
		return verbose;
	}

	/*
	 * Toggles between quiet mode and verbose mode
	 */
	public void toggleMode() {
		if (verbose)
			verbose = false;
		else
			verbose = true;
	}

	public void printAck(byte[] ack) {
		System.out.println("\nClient: Recieved packet is ACK: ");
		System.out.println("Block#: " + ack[2] + ack[3]);
	}

	public void printError(byte[] error) {
		System.out.println("\nClient: Recieved packet is ERROR: ");

		// get the error message
		byte[] errorMsg = new byte[error.length - 5];
		System.arraycopy(error, 4, errorMsg, 0, error.length - 5);
		String message = null;
		try {
			message = new String(errorMsg, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		// display error code to user
		byte errorCode = error[3]; // get error code
		if (errorCode == 1) {
			System.out.println("Error Code: 01: File not found. ");
		} else if (errorCode == 2) {
			System.out.println("Error Code: 02: Access violation. ");
		} else if (errorCode == 3) {
			System.out.println("Error Code: 03: Disk full or allocation exceeded. ");
		} else if (errorCode == 4) {
			System.out.println("Error Code: 04: Illegal TFTP Operation.");
		} else if (errorCode == 5) {
			System.out.println("Error Code: 05: Unknown transfer ID.");
		} else if (errorCode == 6) {
			System.out.println("Error Code: 06: File already exists. ");
		} else {
			System.out.println("Invalid error code");
		}

		// display error message to user
		System.out.println("Error message:" + message);
	}

	/*
	 * returns the byte array of the data in the tftp data packet
	 */
	public byte[] parseData(byte[] data, int dataLength) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(data, 4, dataLength - 4);
		return data = stream.toByteArray();
	}

	public static void main(String[] args) throws Exception {
		Client c = new Client();
		System.out.println("Welcome to SYSC3303-groupproject TFTP Client \n");
		Scanner s = new Scanner(System.in);
		String input;

		while (true) {
			System.out.println("Enter a command (For list of commands type 'help'):");
			input = s.nextLine().toLowerCase();
			String[] cmd = input.split("\\s+");

			if (cmd.length == 0) {
				continue;
			}
			if (cmd[0].equals("help")) {
				printHelpCommand();
			} else if (cmd[0].equals("quit")) {
				System.out.println("Client is shutting down");
				s.close();
				return;
			} else if ((cmd[0].equals("read") || cmd[0].equals("write")) && cmd[1].length() > 0) {
				c.establishConnection(cmd[1], cmd[0]);

				if (c.isConnected()) {
					if (cmd[0].equals("read")) {
						c.readFile(cmd[1]);
					} else
						c.writeFile(cmd[1]);
				} else {
					System.out.println("Unable to connect to server");
				}

			} else if (cmd[0].equals("mode")) {
				c.toggleMode();
				if (c.getMode())
					System.out.println("Client is now in verbose mode");
				else
					System.out.println("Client is now in quiet mode");
			} else if (cmd[0].equals("dir")) {
				System.out.println("The current directory is: " + c.getDirectory());
			} else if (cmd[0].equals("test")) {
				c.toggleTest();
				if (c.getTest())
					System.out.println("Client is now in test mode");
				else
					System.out.println("Client is now in normal mode");
			} else if (cmd[0].equals("port")) {
				System.out.println("The destination port is: " + c.REQUEST_PORT);
			} else {
				System.out.println("Invalid command: The available commands are:");
				printHelpCommand();
			}

		}

	}

	private static void printHelpCommand() {
		System.out.println("read filename - read file from server");
		System.out.println("write filename - write file to server");
		System.out.println("quit - stops the client");
		System.out.println("mode - Toggles between quiet and verbose mode");
		System.out.println("dir - prints the current directory for file transfers");
		System.out.println("test - Toggles between normal and test mode");
		System.out.println("port - Outputs the destination port");
	}

	public void toggleTest() {
		if (test) {
			test = false;
			REQUEST_PORT = 69;
		} else {
			test = true;
			REQUEST_PORT = 23;
		}
	}

	public boolean getTest() {
		return test;
	}

	public String getDirectory() {
		return filePath;
	}

}
