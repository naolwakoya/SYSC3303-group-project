import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.SyncFailedException;
import java.net.*;
import java.util.Scanner;

public class Client {

	int REQUEST_PORT = 69;
	InetAddress serverAddress;
	int sourceTID, destinationTID;

	int resendAttempts = 4;

	DatagramSocket sendReceiveSocket;
	DatagramPacket receivePacket;
	DatagramPacket sendPacket, resendPacket;

	TftpAck validAck = new TftpAck();
	TftpData validData = new TftpData();

	String mode = "octet";
	String filePath = System.getProperty("user.dir") + "/clientFiles/";
	boolean connected = false;
	boolean verbose = true;
	boolean test = false;

	public Client() {
		try {
			sendReceiveSocket = new DatagramSocket();
			serverAddress = InetAddress.getLocalHost();

		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (UnknownHostException e) {
			// Do nothing
		}
	}

	/**
	 * Sets the inetAddress of the server
	 * 
	 * @param serverAddress
	 */
	public void setServerAddress(InetAddress serverAddress) {
		this.serverAddress = serverAddress;
	}

	/**
	 * Establishes a read or write connection with the server according to the TFTP
	 * protocol
	 * 
	 * @param fileName
	 * @param request
	 * @throws IOException
	 */
	public void establishConnection(String fileName, String request) throws IOException {
		try {

			sendReceiveSocket = new DatagramSocket();
			sendReceiveSocket.setSoTimeout(2000);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}

		TftpRequest req = new TftpRequest(fileName, request);
		sendPacket = req.generatePacket(serverAddress, REQUEST_PORT);
		resendPacket = sendPacket;

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

		if (request.equals("read")) {
			try {
				this.receiveExpected(1);
			} catch (Exception e) {
				System.out.println("Connection timed out");
				return;
			}
		} else if (request.equals("write")) {
			try {
				this.receiveExpected(0);
			} catch (Exception e) {
				System.out.println("Connection timed out");
				return;
			}
		}
		connected = true;
		destinationTID = receivePacket.getPort();
	}

	/**
	 * Writes a file to the server
	 * 
	 * @param fileName
	 */
	public void writeFile(String fileName) {
		try {
			String filePath = System.getProperty("user.dir") + "/clientFiles/" + fileName;
			File file = new File(filePath);
			// Check if file exists (error code 1)
			if (!file.exists()) {
				throw new FileNotFoundException();
			}
			// Check for Access violation (error code 2)
			if (!file.canRead()) {
				System.out.println("Cannot read file: " + fileName);
				connected = false;
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
				sendPacket = dataPacket.generatePacket(serverAddress, destinationTID);
				resendPacket = sendPacket;
				System.out.println("\nClient: Sending DATA packet");
				System.out.println("Block#: " + sendPacket.getData()[2] + sendPacket.getData()[3]);

				if (verbose) {
					printPacketInformation(sendPacket);
				}

				try {
					sendReceiveSocket.send(sendPacket);
				} catch (IOException e1) {
					e1.printStackTrace();
					System.exit(1);
				}

				try {
					this.receiveExpected(blockNumber);
				} catch (Exception e) {
					connected = false;
					inputStream.close();
					System.out.println(e.getMessage());
					return;
				}
				// Check if packet is from an unknown transfer ID
				if (receivePacket.getPort() != destinationTID) {
					TftpError error = new TftpError(5, "Unknown transfer ID");
					sendReceiveSocket.send(error.generatePacket(serverAddress, receivePacket.getPort()));
					try {
						this.receiveExpected(blockNumber);
					} catch (Exception e) {
						connected = false;
						inputStream.close();
						System.out.println(e.getMessage());
						return;
					}
				}

				printAck(receivePacket.getData());
				blockNumber++;

			} while (nRead == 512);

			inputStream.close();
			connected = false;

		} catch (FileNotFoundException e) {
			try {
				connected = false;
				TftpError error = new TftpError(1, "Do not have file: " + fileName);
				sendReceiveSocket.send(error.generatePacket(serverAddress, destinationTID));
				System.out.println("Error:  Could not find file " + fileName);
				return;
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (IOException e) {
			connected = false;
			System.out.println("IOException: Failed to send " + fileName + ": " + e.getMessage());
		}

	}

	/**
	 * Reads a file from the server during the file transfer connection
	 * 
	 * @param fileName
	 */
	public void readFile(String fileName) {
		String filePath = System.getProperty("user.dir") + "/clientFiles/" + fileName;
		try {
			File file = new File(filePath);
			// Check access violation (error code 2)
			if (file.exists()) {
				System.out.println("Can't overwrite existing file");
				TftpError error = new TftpError(6, "File already exists, cannot overwrite");
				sendReceiveSocket.send(error.generatePacket(serverAddress, destinationTID));
				connected = false;
				return;
			}
			byte[] fileData;
			FileOutputStream outputStream = new FileOutputStream(filePath);
			int blockNumber = 1;
			try {
				fileData = parseData(receivePacket.getData(), receivePacket.getLength());
				outputStream.write(fileData);
				outputStream.getFD().sync();
			} catch (SyncFailedException e) {
				file.delete();
				outputStream.close();
				TftpError error = new TftpError(3, "Disk full or allocation exceeded");
				sendReceiveSocket.send(error.generatePacket(serverAddress, destinationTID));
				connected = false;
				return;
			}
			// Send acknowledgement packet
			TftpAck ack = new TftpAck(blockNumber);
			sendPacket = ack.generatePacket(serverAddress, destinationTID);
			resendPacket = sendPacket;
			System.out.println("\nClient: Sending ACK packet");
			System.out.println("Block#: " + sendPacket.getData()[2] + sendPacket.getData()[3]);

			blockNumber++;

			if (verbose) {
				printPacketInformation(sendPacket);
			}

			try {
				sendReceiveSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			do {
				try {
					this.receiveExpected(blockNumber);
				} catch (Exception e1) {
					connected = false;
					outputStream.close();
					file.delete();
					System.out.println(e1.getMessage());
					return;
				}
				// Check if packet is from an unknown transfer ID
				if (receivePacket.getPort() != destinationTID) {
					TftpError error = new TftpError(5, "Unknown transfer ID");
					sendReceiveSocket.send(error.generatePacket(serverAddress, receivePacket.getPort()));
					try {
						this.receiveExpected(blockNumber);
					} catch (Exception e) {
						connected = false;
						outputStream.close();
						file.delete();
						System.out.println(e.getMessage());
						return;
					}
				}

				try {
					fileData = parseData(receivePacket.getData(), receivePacket.getLength());
					outputStream.write(fileData);
					outputStream.getFD().sync();
				} catch (SyncFailedException e) {
					file.delete();
					outputStream.close();
					TftpError error = new TftpError(3, "Disk full or allocation exceeded");
					sendReceiveSocket.send(error.generatePacket(serverAddress, destinationTID));
					connected = false;
					return;
				}
				// Send acknowledgement packet
				ack = new TftpAck(blockNumber);
				sendPacket = ack.generatePacket(serverAddress, destinationTID);
				resendPacket = sendPacket;
				System.out.println("\nClient: Sending ACK packet");
				System.out.println("Block#: " + sendPacket.getData()[2] + sendPacket.getData()[3]);

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
			connected = false;

		} catch (IOException e) {
			System.out.println("IOException: Failed to send " + fileName + ": " + e.getMessage());
			new File(filePath).delete();
			connected = false;
			return;
		}
	}

	/**
	 * Receives a packet from the sendReceiveSocket
	 * 
	 * @throws SocketTimeoutException
	 */
	public void receive() throws SocketTimeoutException {
		// Create a DatagramPacket for receiving packets
		byte receive[] = new byte[1024];
		receivePacket = new DatagramPacket(receive, receive.length);

		try {
			// Block until a datagram is received via sendReceiveSocket.
			sendReceiveSocket.receive(receivePacket);
		} catch (IOException e) {
			throw new SocketTimeoutException();
		}

		if (verbose) {
			System.out.println("Received packet:");
			printPacketInformation(receivePacket);
		}
	}

	/**
	 * Verifies that the client is receiving the expected packet from the server
	 * 
	 * @param blockNumber
	 * @throws Exception
	 */
	public void receiveExpected(int blockNumber) throws Exception {
		int timeouts = 0;
		int block;
		try {
			while (true) {
				try {
					this.receive();
					block = ((receivePacket.getData()[2] << 8) & 0xFF00) | (receivePacket.getData()[3] & 0xFF);
					// Check if it is a data packet
					if (receivePacket.getData()[1] == 3) {
						if (block == blockNumber) {
							// Check if not a valid data packet
							if (!validData.validateFormat(receivePacket.getData(), receivePacket.getLength())) {
								TftpError error = new TftpError(4, "Invalid data packet");
								sendReceiveSocket.send(error.generatePacket(serverAddress, destinationTID));
								throw new TftpException("Received an invalid data packet");
							} else {
								return;
							}
						} else if (block < blockNumber) {
							// Received an old data packets, so we are echoing
							// the ack
							TftpAck ack = new TftpAck(receivePacket.getData()[3]);
							sendPacket = ack.generatePacket(serverAddress, destinationTID);
							System.out.println("\nClient: Sending ACK packet in response to duplicate data");
							System.out.println("Block#: " + sendPacket.getData()[2] + sendPacket.getData()[3]);
							sendReceiveSocket.send(sendPacket);

						} else {
							// Received a future block which is invalid
							TftpError error = new TftpError(4, "Invalid block number");
							sendReceiveSocket.send(error.generatePacket(serverAddress, destinationTID));
						}
						// Check to see if it is an ack packet
					} else if (receivePacket.getData()[1] == 4) {
						if (block == blockNumber) {
							// Check if not a valid ack packet
							if (!validAck.validateFormat(receivePacket.getData(), receivePacket.getLength())) {
								TftpError error = new TftpError(4, "Invalid ack packet");
								sendReceiveSocket.send(error.generatePacket(serverAddress, destinationTID));
								throw new TftpException("Received an invalid ack packet");
							} else {
								return;
							}
						} else if (block > blockNumber) {
							// Received a future block which is invalid
							TftpError error = new TftpError(4, "Invalid block number");
							sendReceiveSocket.send(error.generatePacket(serverAddress, destinationTID));
						}
					} else if (receivePacket.getData()[1] == 5) {
						printError(receivePacket.getData(), receivePacket.getLength());
						if (receivePacket.getData()[3] != 5)
							throw new TftpException("Unable to run due to error");
						return;

					} else if (receivePacket.getData()[1] == 1 || receivePacket.getData()[1] == 2) {
						throw new TftpException("Received request packet during data transfer");
					} else {
						// Send an error packet
						TftpError error = new TftpError(4, "Invalid opcode");
						sendReceiveSocket.send(error.generatePacket(serverAddress, destinationTID));
						throw new TftpException("Received a packet with Invalid opcode");
					}

				} catch (SocketTimeoutException e) {
					if (timeouts >= resendAttempts) {
						throw new TftpException("Connection timed out");
					}
					timeouts++;
					resendLastPacket();
				}
			}
		} catch (IOException e) {
			throw new TftpException(e.getMessage());
		}
	}

	/**
	 * Resends the last sent packet
	 * 
	 * @throws Exception
	 */
	private void resendLastPacket() throws Exception {
		System.out.println("Retransmitting last packet");
		if (verbose) {
			printPacketInformation(resendPacket);
		}
		try {
			sendReceiveSocket.send(resendPacket);
		} catch (IOException e) {
			throw new Exception(e.getMessage());
		}
	}

	/**
	 * Processes the received Datagram and Prints the packet information onto the
	 * console
	 */
	public void printPacketInformation(DatagramPacket packet) {
		System.out.println("Host: " + packet.getAddress());
		System.out.println("Host port: " + packet.getPort());
		System.out.println("Packet length: " + packet.getLength());
		System.out.println("Containing: " + packet.getData());
		String packetString = new String(packet.getData(), 0, packet.getLength());
		System.out.println("String form: " + packetString + "\n");
	}

	/**
	 * Checks if the client/server are connected
	 * 
	 * @return true if connected
	 */
	public boolean isConnected() {
		if (connected == true)
			return true;
		else
			return false;
	}

	/**
	 * returns true if in verbose mode and false if in quiet mode
	 */
	public boolean getMode() {
		return verbose;
	}

	/**
	 * Toggles between quiet mode and verbose mode
	 */
	public void toggleMode() {
		if (verbose)
			verbose = false;
		else
			verbose = true;
	}

	/**
	 * Outputs packet information about the received ack packet
	 * 
	 * @param ack
	 */
	public void printAck(byte[] ack) {
		System.out.println("Client: Received ACK Packet ");
		System.out.println("Block#: " + ack[2] + ack[3]);
	}

	/**
	 * Outputs packet information about the received error packet
	 * 
	 * @param error
	 * @param packetLength
	 */
	public void printError(byte[] error, int packetLength) {
		System.out.println("Client: Received ERROR Packet ");
		// display error code to user
		int errorCode = error[3]; // get error code
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
		// get the error message
		StringBuilder errorMsg = new StringBuilder();
		for (int x = 4; x < (packetLength - 1); x++) {
			if (error[x] == 0)
				throw new IllegalArgumentException();
			else
				errorMsg.append((char) error[x]);
		}
		// display error message to user
		System.out.println("Error message:" + errorMsg.toString());
	}

	/**
	 * @param data
	 * @param dataLength
	 * @return the byte array of the data in the tftp data packet
	 */
	public byte[] parseData(byte[] data, int dataLength) {
		System.out.println("Client: Received DATA packet ");
		System.out.println("Block#: " + data[2] + data[3]);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(data, 4, dataLength - 4);
		return data = stream.toByteArray();
	}

	public static void main(String[] args) throws Exception {
		Client c = new Client();
		System.out.println("Welcome to SYSC3303-groupproject TFTP Client");
		System.out.println("The client is in verbose-normal mode\n");
		Scanner s = new Scanner(System.in);
		String input;

		while (true) {
			System.out.println("\nEnter a command (For list of commands type 'help'):");
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
			} else if ((cmd[0].equals("read") || cmd[0].equals("write")) && cmd.length > 1 && cmd[1].length() > 0) {
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
			} else if (cmd[0].equals("host")) {
				System.out.println("server: " + c.getServerAddress() + " port: " + c.getPort());
			} else if ((cmd[0].equals("server"))) {
				System.out.println("Enter the ip or hostname");
				input = s.nextLine().toLowerCase();
				c.setServerAddress(InetAddress.getByName(input));
				System.out.println("The client will now send to server: " + c.getServerAddress());
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
		System.out.println("host - Outputs the server address and port");
		System.out.println("server - set the ip or hostname of the server\n");
	}

	public String getServerAddress() {
		return serverAddress.getHostName() + "/" + serverAddress.getHostAddress();
	}

	public int getPort() {
		return REQUEST_PORT;
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
