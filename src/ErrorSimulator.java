
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ErrorSimulator {
	// instance variables
	private DatagramSocket sendReceiveSocket, receiveSocket;
	private DatagramPacket receivePacket, sendPacket;

	boolean isConnected = false;

	InetAddress clientAddress;
	int clientPort, serverPort;

	int proxyPort = 8080;
	int serverRequestPort = 8081;

	boolean actionPerformed = false;

	public ErrorSimulator() {
		// create new datagram sockets for the client and server
		try {
			receiveSocket = new DatagramSocket(proxyPort, InetAddress.getLocalHost());
		} catch (IOException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	public void run() {
		this.receiveRequest();
		//Set the source TID 
		clientPort = receivePacket.getPort();

		try {
			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), InetAddress.getLocalHost(),
					serverRequestPort);
			System.out.println("Forwarding packet to server on port " + sendPacket.getPort());
			sendReceiveSocket.send(sendPacket);
			System.out.println("Packet forwarded.");
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.exit(1);
		}

		this.receive();
		//Set the destination TID
		serverPort = receivePacket.getPort();
		// Forward the packet to the client 
		try {
			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), InetAddress.getLocalHost(),
					clientPort);
			System.out.println("Forwarding packet to client on port " + sendPacket.getPort());
			sendReceiveSocket.send(sendPacket);
			System.out.println("Packet forwarded.");
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.exit(1);
		}

		while(true) {
			this.receive();
			// Forward the packet to the server
			try {
				sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), InetAddress.getLocalHost(),
						serverPort);
				System.out.println("Forwarding packet to server on port " + sendPacket.getPort());
				sendReceiveSocket.send(sendPacket);
				System.out.println("Packet forwarded.");
			} catch (IOException ioe) {
				ioe.printStackTrace();
				System.exit(1);
			}

			this.receive();
			// Forward the packet to the client
			try {
				sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), InetAddress.getLocalHost(),
						clientPort);
				System.out.println("Forwarding packet to client on port " + sendPacket.getPort());
				sendReceiveSocket.send(sendPacket);
				System.out.println("Packet forwarded.");
			} catch (IOException ioe) {
				ioe.printStackTrace();
				System.exit(1);
			}
		}

	}

	// method to return the type of packet received
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

	private void forward() {

		this.receive();

		System.out.println("\n");

		String packetType = "";
		int whatDo = 0; // default action (do nothing)
		// if no user input has been set, it will enter this if statement and ask the
		// user what they want to do
		if (!actionPerformed) {
			packetType = getPacketType(data);
			whatDo = getOperation();
			actionPerformed = true;
		}

		// receive packet from client and decide what to do with it.
		if (whatDo == 0) {
			// no nothing, simply forward packet thats been received
			System.out.println("Forwarding packet without altering it");
			try {
				sendPacket = new DatagramPacket(data, receivePacket.getLength(), InetAddress.getLocalHost(),
						serverRequestPort);
				System.out.println("Forwarding packet to server on port " + sendPacket.getPort());
				sendReceiveSocket.send(sendPacket);
				System.out.println("Packet forwarded.");
			} catch (IOException ioe) {
				ioe.printStackTrace();
				System.exit(1);
			}
		} else if (packetType.equals("request") && whatDo == 4) {
			// takes the request packets and alters the opcode to an invalid
			// opcode
			System.out.println("Altering opcode of request packet...");
			data[0] = 9;
			data[1] = 9;
			System.out.println("OPCODE changed to: " + data[0] + data[1]);
			try {
				sendPacket = new DatagramPacket(data, receivePacket.getLength(), InetAddress.getLocalHost(),
						serverRequestPort);
				System.out.println("Forwarding packet to server on port " + sendPacket.getPort());
				sendReceiveSocket.send(sendPacket);
				System.out.println("Packet forwarded.");
			} catch (IOException ioe) {
				ioe.printStackTrace();
				System.exit(1);
			}
		} else if (packetType.equals("request") && whatDo == 5) {
			// takes the request packet and alters the filename
			System.out.println("Altering filename of request packet...");
			String fileName = extractFileName(data, data.length);
			System.out.println("Original filename: " + fileName);
			System.out.println("Changing fileName to: randomFileName");

			// create new byteArray with different fileName
			fileName = " randomFileName";
			String mode = "ascii";
			ByteArrayOutputStream request = new ByteArrayOutputStream();
			// hardcode request bytearraystream to be later converted to byte
			// array
			request.write(data[0]);
			request.write(data[1]);
			request.write(fileName.getBytes(), 0, fileName.getBytes().length);
			request.write(0);
			request.write(mode.getBytes(), 0, mode.getBytes().length);
			request.write(0);

			byte[] msg = request.toByteArray();

			// put byteArray into packet and forward to server
			try {
				sendPacket = new DatagramPacket(msg, receivePacket.getLength(), InetAddress.getLocalHost(),
						serverRequestPort);
				System.out.println("Forwardging packet to server on port " + sendPacket.getPort());
				sendReceiveSocket.send(sendPacket);
				System.out.println("Packet forwarded");
			} catch (IOException ioe) {
				ioe.printStackTrace();
				System.exit(1);
			}
		} else if (packetType.equals("request") && whatDo == 6) {
			// takes the request packet and alters the mode
			System.out.println("option to change request packet's mode has been chosen");
			System.out.println("Altering mode of request packet...");
			System.out.println("Changing mode to: randomMode");

			// create new byteArray with different fileName
			String fileName = extractFileName(data, data.length);
			String mode = "ascii";
			ByteArrayOutputStream request = new ByteArrayOutputStream();
			// hardcode request bytearraystream to be later converted to byte
			// array
			request.write(data[0]);
			request.write(data[1]);
			request.write(fileName.getBytes(), 0, fileName.getBytes().length);
			request.write(0);
			request.write(mode.getBytes(), 0, mode.getBytes().length);
			request.write(0);

			byte[] msg = request.toByteArray();

			// put byteArray into packet and forward to server
			try {
				sendPacket = new DatagramPacket(msg, receivePacket.getLength(), InetAddress.getLocalHost(),
						serverRequestPort);
				System.out.println("Forwardging packet to server on port " + sendPacket.getPort());
				sendReceiveSocket.send(sendPacket);
				System.out.println("Packet forwarded.");
			} catch (IOException ioe) {
				ioe.printStackTrace();
				System.exit(1);
			}
		} else if (whatDo == 7) {
			// if chosen to edit data packet, this if branch will run
			// packet received from client will just be forwarded so the data packet from
			// the server can be later altered
			System.out.println("Option to change data packets chosen.");
			try {
				sendPacket = new DatagramPacket(data, receivePacket.getLength(), InetAddress.getLocalHost(),
						serverRequestPort);
				System.out.println("Forwarding packet to server on port " + sendPacket.getPort());
				sendReceiveSocket.send(sendPacket);
				System.out.println("Packet forwarded.");
			} catch (IOException ioe) {
				ioe.printStackTrace();
				System.exit(1);
			}
		}

		try {
			System.out.println("\n");

			// receive response from server
			data = new byte[516];
			receivePacket = new DatagramPacket(data, data.length);
			System.out.println("Receiving...");
			sendReceiveSocket.receive(receivePacket);
			System.out.println("Packet received from server on port " + receivePacket.getPort());
			serverRequestPort = receivePacket.getPort();

			// get the type of packet thats been received from the server
			packetType = getPacketType(data);

			if (packetType.equals("data") && whatDo == 7) {
				// if chosen to edit data packet, this if branch will run
				System.out.println("Changing data packets opcode...");
				data[0] = 9;
				data[1] = 9;
				System.out.println("OPCODE changed to: " + data[0] + data[1]);
				try {
					sendPacket = new DatagramPacket(data, receivePacket.getLength(), clientAddress, clientPort);
					System.out.println("Forwarding packet to client on port " + sendPacket.getPort());
					sendReceiveSocket.send(sendPacket);
					System.out.println("Packet forwarded.");
				} catch (IOException ioe) {
					ioe.printStackTrace();
					System.exit(1);
				}
			} else {
				// forward packet to client
				sendPacket = new DatagramPacket(data, receivePacket.getLength(), clientAddress, clientPort);
				System.out.println("Forwarding packet back to client...");
				sendReceiveSocket.send(sendPacket);
				System.out.println("Packet forwarded. \n");
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

	private int getOperation() {
		return;
	}


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
		int input;

		while (true) {
			System.out.println("What would you like change?");
			System.out.println("(0): normal operation");
			System.out.println("(1): lose a packet");
			System.out.println("(2): delay a packet");
			System.out.println("(3): duplicate a packet");

			System.out.println("\n");
			input = s.nextInt();

			if (input == 0) {
				// do nothing
				System.out.println("(0): Confirm do nothing");
				er.run();
			} else if (input == 1) {
				System.out.println("(1)Request packets chosen.");
				System.out.println("What would you like to do to the request packets?");
				System.out.println("(4): change opcode");
				System.out.println("(5): change fileName");
				System.out.println("(6): change mode");
			} else if (input == 2) {
				System.out.println("(2)Data Packets chosen.");
				System.out.println("What would you like to do to the Data packets?");
				System.out.println("(7): change opcode");
				System.out.println("(8): change block number");
			} else if (input == 3) {
				System.out.println("(3)Acknowledgement Packets chosen.");
				System.out.println("What would you like to do to the Ack packets?");
				System.out.println("(9):  change opcode");
				System.out.println("(10): change block number");
			}
		}
	}

}