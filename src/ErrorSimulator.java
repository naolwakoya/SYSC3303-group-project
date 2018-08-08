
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ErrorSimulator {
	// instance variables
	private DatagramSocket sendReceiveSocket, receiveSocket;
	private DatagramPacket receivePacket, sendPacket;

	boolean isConnected = false;

	boolean running = true;
	boolean lastAck, lastData;

	InetAddress clientAddress, serverAddress;
	int clientPort, serverPort;
	int proxyPort = 23;
	int serverRequestPort = 69;

	private int blockNumber, newBlockNumber, operation;

	boolean actionPerformed = false;

	public ErrorSimulator() {
		// create new datagram sockets for the client and server
		try {
			receiveSocket = new DatagramSocket(proxyPort, InetAddress.getLocalHost());
			serverAddress = InetAddress.getLocalHost();
		} catch (IOException se) {
			se.printStackTrace();
			System.exit(1);
		}
		lastAck = false;
		lastData = false;
	}

	/**
	 * Forwards and modifies TFTP packets during file transfer between the
	 * client/server
	 * 
	 * @param operation
	 * @param blockNumber
	 * @param newBlockNumber
	 */
	public void run(int operation, int blockNumber, int newBlockNumber) {
		// Set the sendReceive socket
		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException e) {
		}
		this.blockNumber = blockNumber;
		this.newBlockNumber = newBlockNumber;
		this.operation = operation;

		// Receive request from client
		this.receiveRequest();
		// Set the source TID
		clientPort = receivePacket.getPort();
		clientAddress = receivePacket.getAddress();
		// Create packet to forward to the server
		sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), serverAddress,
				serverRequestPort);
		this.forwardRequestPacket();

		// Receive response from server
		this.receive();
		// Set the destination TID
		serverPort = receivePacket.getPort();
		// Create packet to forward to client
		sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), clientAddress, clientPort);
		this.forwardPacket();

		// Will run for the entire connection file transfer unless an error
		// occurs
		while (!(lastData&&lastAck) && running) {
			this.receive();
			// Forward the packet to the server
			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), serverAddress,
					serverPort);
			System.out.println("Forwarding packet to server on port " + sendPacket.getPort());
			this.forwardPacket();
			if (!running || (lastAck&&lastData)) {
				return;
			}

			this.receive();
			// Forward the packet to the client
			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), clientAddress,
					clientPort);
			System.out.println("Forwarding packet to client on port " + sendPacket.getPort());
			this.forwardPacket();
		}
	}

	/**
	 * Forwards the request packet to the client/server with packet
	 * modifications by the error simulator
	 */
	private void forwardRequestPacket() {
		// Modify request to have invalid opcode
		if (operation == 1) {
			this.send(invalidOpcode(sendPacket));
		}
		// Modify request to have invalid mode
		else if (operation == 2) {
			this.send(invalidMode(sendPacket));
		}
		// Modify request to have invalid format
		else if (operation == 3) {
			this.send(invalidRequestFormat(sendPacket));
		}
		// Normal operation
		else {
			this.send(sendPacket);
		}
	}

	/**
	 * Forwards the packet to the client/server with packet modifications by the
	 * error simulator
	 */
	private void forwardPacket() {
		// Check if it is the last data packet
		if (receivePacket.getData()[1] == 3 && receivePacket.getLength()<516) {
			lastData = true;
		}
		// Check if it is the last ack packet
		if (receivePacket.getData()[1] == 4 && lastData) {
			lastAck = true;
		}
		// If the packet is an error packet forward the packet and do not run
		// the file transfer
		if (receivePacket.getData()[1] == 5) {
			this.send(sendPacket);
			sendReceiveSocket.close();
			running = false;
			return;
		}
		// Check if packet is a data packet that we want to modify
		else if (receivePacket.getData()[1] == 3 && receivePacket.getData()[3] == blockNumber) {
			// Modify data to have invalid opcode
			if (operation == 4) {
				this.send(invalidOpcode(sendPacket));
			}
			// Modify data to have invalid format (>516 bytes)
			else if (operation == 5) {
				this.send(invalidDataFormat(sendPacket));
			}
			// Change block number of data packet
			else if (operation == 6) {
				this.send(changeBlockNumber(sendPacket));
			}
			// Normal operation
			else {
				this.send(sendPacket);
			}
		}
		// Check if packet is an ack packet that we want to modify
		else if (receivePacket.getData()[1] == 4 && receivePacket.getData()[3] == blockNumber) {
			// Modify ack to have invalid opcode
			if (operation == 7) {
				this.send(invalidOpcode(sendPacket));
			}
			// Modify ack to have invalid format (>4 bytes)
			else if (operation == 8) {
				this.send(invalidAckFormat(sendPacket));
			}
			// Change block number of ack packet
			else if (operation == 9) {
				this.send(changeBlockNumber(sendPacket));
			}
			// Normal operation
			else {
				this.send(sendPacket);
			}
		}
		// Normal operation
		else {
			this.send(sendPacket);
		}

	}

	/**
	 * Changes the opcode of the packet to an invalid TFTP opcode
	 * 
	 * @param packet
	 * @return
	 */
	private DatagramPacket invalidOpcode(DatagramPacket packet) {
		byte[] data = packet.getData();
		data[1] = 9;
		return new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
	}

	/**
	 * Changes the format of a request packet to an invalid format
	 * 
	 * @param packet
	 * @return
	 */
	private DatagramPacket invalidRequestFormat(DatagramPacket packet) {
		return new DatagramPacket(packet.getData(), packet.getLength() - 1, packet.getAddress(), packet.getPort());
	}

	/**
	 * Changes the mode of the request packet to an invalid mode
	 * 
	 * @param packet
	 * @return
	 */
	private DatagramPacket invalidMode(DatagramPacket packet) {
		byte[] data = packet.getData();
		int x = 1;
		while (data[++x] != 0 && x < packet.getLength())
			;

		byte[] mode = ("invalid").getBytes();
		x++;
		for (int i = 0; i < mode.length; i++) {
			data[x + i] = mode[i];
		}
		return new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
	}

	/**
	 * Makes the data packet larger than 516 bytes
	 * 
	 * @param packet
	 * @return
	 */
	private DatagramPacket invalidDataFormat(DatagramPacket packet) {
		byte[] data = packet.getData();
		int i = 516;
		while (i < 1024) {
			data[i] = (byte) 1;
			i++;
		}
		return new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
	}

	/**
	 * Makes the ack packet larger than 4 bytes
	 * 
	 * @param packet
	 * @return
	 */
	private DatagramPacket invalidAckFormat(DatagramPacket packet) {
		byte[] data = packet.getData();
		byte[] newData = new byte[data.length + 1];
		System.arraycopy(data, 0, newData, 0, data.length);
		newData[newData.length] = 1;
		return new DatagramPacket(newData, newData.length, packet.getAddress(), packet.getPort());
	}

	/**
	 * Changes the block number of the TFTP packet
	 * 
	 * @param packet
	 * @return
	 */
	private DatagramPacket changeBlockNumber(DatagramPacket packet) {
		byte[] data = packet.getData();
		data[2] = (byte) ((newBlockNumber >> 8) & 0xFF);
		data[3] = (byte) (newBlockNumber & 0xFF);

		return new DatagramPacket(data, packet.getLength(), packet.getAddress(), packet.getPort());
	}

	/**
	 * Returns the type of TFTP packet
	 * 
	 * @param data
	 * @return
	 */
	private String getPacketType(byte[] data) {
		if (data[1] == 1 || data[1] == 2) {
			return "request";
		} else if (data[1] == 3) {
			return "data";
		} else if (data[1] == 4) {
			return "ack";
		}
		return "error";
	}

	/**
	 * method to send packet
	 * 
	 * @param data
	 */
	private void send(DatagramPacket packet) {
		try {
			sendReceiveSocket.send(packet);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Packet forwarded.");
	}

	/**
	 * Waits to receive a packet from the sendReceiveSocket with random port
	 */
	public void receive() {
		// Create a DatagramPacket for receiving packets
		byte receive[] = new byte[516];
		receivePacket = new DatagramPacket(receive, receive.length);

		try {
			// Block until a datagram is received via sendReceiveSocket.
			sendReceiveSocket.receive(receivePacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Waits to receive a request from the receiveSocket on port 23
	 */
	public void receiveRequest() {
		// Create a DatagramPacket for receiving packets
		byte receive[] = new byte[516];
		receivePacket = new DatagramPacket(receive, receive.length);

		try {
			// Block until a datagram is received via receiveSocket.
			receiveSocket.receive(receivePacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Extracts the filename from the TFTP request packet
	 * 
	 * @param data
	 * @param dataLength
	 * @return
	 */
	private String extractFileName(byte[] data, int dataLength) {
		int i = 1;
		StringBuilder sb = new StringBuilder();
		while (data[++i] != 0 && i < dataLength) {
			sb.append((char) data[i]);
		}

		return sb.toString();
	}

	public static void main(String[] args) {
		Scanner s = new Scanner(System.in);
		ErrorSimulator er = new ErrorSimulator();
		System.out.println("Error Simulator");
		int input, operation;
		int blockNumber = 0;
		int newBlockNumber = 0;

		while (true) {
			System.out.println("What would you like to change?");
			System.out.println("(0): normal operation");
			System.out.println("(1): request packet");
			System.out.println("(2): data packet");
			System.out.println("(3): ack packet");
			System.out.println("(10): lose a packet");
			System.out.println("(11): delay a packet");
			System.out.println("(12): duplicate a packet");
			System.out.println("(13): Invalid TID");

			System.out.println("\n");
			input = s.nextInt();

			if (input == 0) {
				// do nothing
				System.out.println("(0): Confirm do nothing");
				operation = s.nextInt();
				er.run(operation, blockNumber, newBlockNumber);
			} else if (input == 1) {
				System.out.println("(1)Request packets chosen.");
				System.out.println("What would you like to do to the request packet?");
				System.out.println("(1): invalid opcode");
				System.out.println("(2): invalid mode");
				System.out.println("(3): invalid format (missing 0 after mode/missing 0 after filename)");
				operation = s.nextInt();
				er.run(operation, blockNumber, newBlockNumber);
			} else if (input == 2) {
				System.out.println("(2)Data Packets chosen.");
				System.out.println("Which DATA packet would you like to change (block#)");
				blockNumber = s.nextInt();
				System.out.println("What would you like to do to the Data packet?");
				System.out.println("(4): invalid opcode");
				System.out.println("(5): invalid data format (>516)");
				System.out.println("(6): change block number");
				operation = s.nextInt();
				if (operation == 6) {
					System.out.println("What new block number?");
					newBlockNumber = s.nextInt();
					er.run(operation, blockNumber, newBlockNumber);
				} else {
					er.run(operation, blockNumber, newBlockNumber);
				}
			} else if (input == 3) {
				System.out.println("(3)Acknowledgement Packets chosen.");
				System.out.println("Which ACK packet would you like to change (block#)");
				blockNumber = s.nextInt();
				System.out.println("What would you like to do to the Ack packets?");
				System.out.println("(7): invalid opcode");
				System.out.println("(8): invalid ack format (>4)");
				System.out.println("(9): change block number");
				operation = s.nextInt();
				if (operation == 9) {
					System.out.println("What new block number?");
					newBlockNumber = s.nextInt();
					er.run(operation, blockNumber, newBlockNumber);
				} else {
					er.run(operation, blockNumber, newBlockNumber);
				}
			} else if (input == 10) {
				System.out.println("(10)Lose a packet chosen.");
				System.out.println("Which packet do you want to lose? (eg. RRQ, 2 DATA, 3 ACK)");
				operation = input;
				er.run(operation, blockNumber, newBlockNumber);
			} else if (input == 11) {
				System.out.println("(11)Delay a packet chosen.");
				System.out.println("Which packet do you want to delay? (eg. RRQ, 2 DATA, 3 ACK)");
				input = s.nextInt();
				System.out.println("How long of a delay?");
			} else if (input == 12) {
				System.out.println("(12)Duplicate a packet chosen.");
				System.out.println("Which packet do you want to duplicate? (eg. RRQ, 2 DATA, 3 ACK)");

				System.out.println("How long of a space between duplicates?");
			} else if (input == 13) {
				System.out.println("(13)Invalid TID chosen.");
				System.out.println("Which packet do you want to have an invalid TID (eg. 2 DATA, 3 ACK)");

			} else if (input > 13) {
				System.out.println("Closing the error simulator");
				return;
			}

		}
	}

}