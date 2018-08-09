
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ErrorSimulator {
	// instance variables
	private DatagramSocket sendReceiveSocket, receiveSocket;
	private DatagramPacket receivePacket, sendPacket;

	boolean running = true;
	boolean lastAck, lastData, losePacket;

	InetAddress clientAddress, serverAddress;
	int clientPort, serverPort;
	int proxyPort = 23;
	int serverRequestPort = 69;

	private int blockNumber, newBlockNumber, operation;
	private int delay;

	public ErrorSimulator() {
		// create new datagram sockets for the client and server
		try {
			receiveSocket = new DatagramSocket(proxyPort, InetAddress.getLocalHost());
			serverAddress = InetAddress.getLocalHost();
		} catch (IOException se) {
			se.printStackTrace();
			System.exit(1);
		}
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
		// Reset boolean values
		lastAck = false;
		lastData = false;
		running = true;
		losePacket = true;
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
		System.out.println("Received packet from client on port " + receivePacket.getPort());
		printPacketInformation(receivePacket);
		// Set the source TID
		clientPort = receivePacket.getPort();
		clientAddress = receivePacket.getAddress();
		// Create packet to forward to the server
		sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), serverAddress,
				serverRequestPort);
		System.out.println("Forwarding packet to server on port " + sendPacket.getPort());
		this.forwardRequestPacket();

		// Receive response from server
		this.receive();
		System.out.println("Received packet from server on port " + receivePacket.getPort());
		printPacketInformation(receivePacket);
		// Set the destination TID
		serverPort = receivePacket.getPort();
		// forward packet to client
		sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), clientAddress, clientPort);
		System.out.println("Forwarding packet to client on port " + sendPacket.getPort());
		this.forwardPacket();

		// Will run for the entire connection file transfer unless an error
		// occurs
		while (!(lastData && lastAck) && running) {
			// Receive packet from client
			this.receive();
			System.out.println("Received packet from client on port " + receivePacket.getPort());
			printPacketInformation(receivePacket);
			// Forward the packet
			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), serverAddress,
					serverPort);
			System.out.println("Forwarding packet to server on port " + sendPacket.getPort());
			this.forwardPacket();
			if (!running || (lastAck && lastData)) {
				sendReceiveSocket.close();
				return;
			}
			// Receive packet
			this.receive();
			System.out.println("Received packet from server on port " + receivePacket.getPort());
			printPacketInformation(receivePacket);
			// Forward the packet
			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), clientAddress,
					clientPort);
			System.out.println("Forwarding packet to client on port " + sendPacket.getPort());
			this.forwardPacket();
		}
		sendReceiveSocket.close();
	}

	/**
	 * Forwards the request packet to the client/server with packet modifications by
	 * the error simulator
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
		// Lose the request packet
		else if (operation == 10 && losePacket) {
			// Do nothing
			losePacket = false;
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
		if (receivePacket.getData()[1] == 3 && receivePacket.getLength() < 516) {
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
				this.send(invalidFormat(sendPacket));
			}
			// Change block number of data packet
			else if (operation == 6) {
				this.send(changeBlockNumber(sendPacket));
			}
			// Lose the data packet
			else if (operation == 11 && losePacket) {
				losePacket = false;
				//Wait for the host to resend the data packet
				this.receive();
				while (receivePacket.getData()[1]!=3) {
					this.receive();
				}
				System.out.println("Received packet on port " + receivePacket.getPort());
				// Forward the packet
				if(receivePacket.getPort()==clientPort) {
				sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), serverAddress,
						serverPort);
				}
				else if (receivePacket.getPort()==serverPort) {
					sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), clientAddress,
							clientPort);
				}
				System.out.println("Forwarding packet on port " + sendPacket.getPort());
				this.forwardPacket();
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
				this.send(invalidFormat(sendPacket));
			}
			// Change block number of ack packet
			else if (operation == 9) {
				this.send(changeBlockNumber(sendPacket));
			}
			// Lose the ack packet
			else if (operation == 12 && losePacket) {
				losePacket = false;
				//Wait for the host to resend the data packet
				this.receive();
				while (receivePacket.getData()[1]!=3) {
					this.receive();
				}
				System.out.println("Received packet on port " + receivePacket.getPort());
				// Forward the packet
				if(receivePacket.getPort()==clientPort) {
				sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), serverAddress,
						serverPort);
				}
				else if (receivePacket.getPort()==serverPort) {
					sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), clientAddress,
							clientPort);
				}
				System.out.println("Forwarding packet on port " + sendPacket.getPort());
				this.forwardPacket();
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
	 * Makes the TFTP packet larger than the maximum size
	 * 
	 * @param packet
	 * @return
	 */
	private DatagramPacket invalidFormat(DatagramPacket packet) {
		byte[] data = packet.getData();
		byte[] data2 = packet.getData();
		byte[] newData = new byte[data.length + data2.length];
		System.arraycopy(data, 0, newData, 0, data.length);
		System.arraycopy(data2, 0, newData, data.length, data2.length);

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
	 * method to send packet
	 * 
	 * @param data
	 */
	private void send(DatagramPacket packet) {
		printPacketInformation(packet);
		try {
			sendReceiveSocket.send(packet);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.exit(1);
		}
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
	 * Processes the received Datagram and Prints the packet information onto the
	 * console
	 */
	public void printPacketInformation(DatagramPacket packet) {
		System.out.println("Host: " + packet.getAddress());
		System.out.println("Packet length: " + packet.getLength());
		// Request packet
		if (packet.getData()[1] == 1 || packet.getData()[1] == 2) {
			System.out.println("Request packet");
		}
		// Data packet
		else if (packet.getData()[1] == 3) {
			System.out.println("Data packet");
			System.out.println("Block#: " + packet.getData()[2] + packet.getData()[3]);
		}
		// Ack packet
		else if (packet.getData()[1] == 4) {
			System.out.println("ACK packet");
			System.out.println("Block#: " + packet.getData()[2] + packet.getData()[3]);
		}
		// Error packet
		else if (packet.getData()[1] == 5) {
			System.out.println("Error packet");
		}
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}

	public static void main(String[] args) {
		Scanner s = new Scanner(System.in);
		ErrorSimulator er = new ErrorSimulator();
		System.out.println("Error Simulator");
		int input, operation, delay;
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
			System.out.println("(any other value): quit");

			input = s.nextInt();

			if (input == 0) {
				// do nothing
				System.out.println("(0): Confirm do nothing");
				operation = s.nextInt();
				if (operation == 0) {
					System.out.println("Performing normal operation");
					er.run(operation, blockNumber, newBlockNumber);
					System.out.println("Operation complete...");
				} else {
					System.out.println("Invalid command");
				}
			} else if (input == 1) {
				System.out.println("(1)Request packets chosen.");
				System.out.println("What would you like to do to the request packet?");
				System.out.println("(1): invalid opcode");
				System.out.println("(2): invalid mode");
				System.out.println("(3): invalid format (missing 0 after mode/missing 0 after filename)");
				operation = s.nextInt();
				if (operation == 1 || operation == 2 || operation == 3) {
					System.out.println("Performing the modified operation");
					er.run(operation, blockNumber, newBlockNumber);
					System.out.println("Operation complete...");
				} else {
					System.out.println("Invalid command");
				}
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
					System.out.println("Change to what block number?");
					newBlockNumber = s.nextInt();
					System.out.println("Performing operation with changed block number");
					er.run(operation, blockNumber, newBlockNumber);
					System.out.println("Operation complete...");
				} else if (operation == 4 || operation == 5) {
					System.out.println("Performing the modified operation");
					er.run(operation, blockNumber, newBlockNumber);
					System.out.println("Operation complete...");
				} else {
					System.out.println("Invalid command");
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
					System.out.println("Change to what block number?");
					newBlockNumber = s.nextInt();
					System.out.println("Performing operation with changed block number");
					er.run(operation, blockNumber, newBlockNumber);
					System.out.println("Operation complete...");
				} else if (operation == 7 || operation == 8) {
					System.out.println("Performing the modified operation");
					er.run(operation, blockNumber, newBlockNumber);
					System.out.println("Operation complete...");
				} else {
					System.out.println("Invalid command");
				}
			} else if (input == 10) {
				System.out.println("(10)Lose a packet chosen.");
				System.out.println("What type of packet do you want to lose?");
				System.out.println("(10): Request");
				System.out.println("(11): DATA");
				System.out.println("(12): ACK");
				operation = s.nextInt();
				if (operation == 10) {
					System.out.println("Performing operation to lose packet");
					er.run(operation, blockNumber, newBlockNumber);
					System.out.println("Operation complete...");
				} else if (operation == 11 || operation == 12) {
					System.out.println("Which packet would you like to lose (block#)");
					blockNumber = s.nextInt();
					System.out.println("Performing operation to lose packet");
					er.run(operation, blockNumber, newBlockNumber);
					System.out.println("Operation complete...");
				} else {
					System.out.println("Invalid command");
				}
			} else if (input == 11) {
				System.out.println("(11)Delay a packet chosen.");
				System.out.println("What type of packet do you want to delay?");
				System.out.println("(13): Request");
				System.out.println("(14): DATA");
				System.out.println("(15): ACK");
				operation = s.nextInt();
				if (operation == 13) {
					System.out.println("How long of a delay? (ms)");
					delay = s.nextInt();
					er.setDelay(delay);
					System.out.println("Performing operation to delay packet");
					er.run(operation, blockNumber, newBlockNumber);
					System.out.println("Operation complete...");
				} else if (operation == 14 || operation == 15) {
					System.out.println("Which packet would you like to delay (block#)");
					blockNumber = s.nextInt();
					System.out.println("How long of a delay? (ms)");
					delay = s.nextInt();
					er.setDelay(delay);
					System.out.println("Performing operation to delay packet");
					er.run(operation, blockNumber, newBlockNumber);
					System.out.println("Operation complete...");
				} else {
					System.out.println("Invalid command");
				}
			} else if (input == 12) {
				System.out.println("(12)Duplicate a packet chosen.");
				System.out.println("What type of packet do you want to duplicate?");
				System.out.println("(16): Request");
				System.out.println("(17): DATA");
				System.out.println("(18): ACK");
				operation = s.nextInt();
				if (operation == 16) {
					System.out.println("How much of a space between duplicates? (ms)");
					delay = s.nextInt();
					er.setDelay(delay);
					System.out.println("Performing operation to duplicate packet");
					er.run(operation, blockNumber, newBlockNumber);
					System.out.println("Operation complete...");
				} else if (operation == 17 || operation == 18) {
					System.out.println("Which packet would you like to duplicate (block#)");
					blockNumber = s.nextInt();
					System.out.println("How much of a space between duplicates? (ms)");
					delay = s.nextInt();
					er.setDelay(delay);
					System.out.println("Performing operation to duplicate packet");
					er.run(operation, blockNumber, newBlockNumber);
					System.out.println("Operation complete...");
				} else {
					System.out.println("Invalid command");
				}
			} else if (input == 13) {
				System.out.println("(13)Invalid TID chosen.");
				System.out.println("What type of packet do you want to have an invalid TID?");
				System.out.println("(19): DATA");
				System.out.println("(20): ACK");
				operation = s.nextInt();
				if (operation == 19 || operation == 20) {
					System.out.println("Which packet would you like to have an invalid TID (block#)");
					blockNumber = s.nextInt();
					System.out.println("Performing operation to for invalid TID");
					er.run(operation, blockNumber, newBlockNumber);
					System.out.println("Operation complete...");
				} else {
					System.out.println("Invalid command");
				}
			} else if (input > 13) {
				System.out.println("Closing the error simulator");
				System.exit(0);
			}

		}
	}

}