import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ErrorSimulator implements Runnable {

	// instance variables
	DatagramSocket receiveSocket, sendReceiveSocket;
	DatagramPacket receivePacket, sendPacket;

	boolean isConnected = false;

	InetAddress clientAddress;
	int clientPort;

	int proxyPort = 23;
	int server1Port = 69;

	boolean actionPerformed = false;

	Scanner input;

	// constructor
	public ErrorSimulator() {
	}

	public void connect() {
		// scanner to receive user input from prompts
		input = new Scanner(System.in);

		// create new datagram sockets for the client and server
		try {
			receiveSocket = new DatagramSocket(proxyPort, InetAddress.getLocalHost());
			System.out.println("Connected to client on port: " + receiveSocket.getLocalPort());
			sendReceiveSocket = new DatagramSocket();
		} catch (IOException se) {
			se.printStackTrace();
			System.exit(1);
		}
		isConnected = true;
	}

	public void run() {
		if (isConnected == false) {
			connect();
		}
		while (true) {
			forward();
		}
	}

	// asks the user what type of operation to perform
	public int getOperation() {
		int response = 9;
		System.out.println("What would you like corrupt?");
		System.out.println("(0): normal operation");
		System.out.println("(1): request packets");
		System.out.println("(2): data packets");
		System.out.println("(3): ack packets");

		response = input.nextInt();

		System.out.println("\n");

		if (response == 0) {
			// do nothing
			System.out.println("(0): Confirm do nothing");
		} else if (response == 1) {
			System.out.println("(1)Request packets chosen.");
			System.out.println("What would you like to do to the request packets?");
			System.out.println("(4): change opcode");
			System.out.println("(5): change fileName");
			System.out.println("(6): change mode");
		} else if (response == 2) {
			System.out.println("(2)Data Packets chosen.");
			System.out.println("What would you like to do to the Data packets?");
			System.out.println("(7): change opcode");
			System.out.println("(8): change block number");
		} else if (response == 3) {
			System.out.println("(3)Acknowledgement Packets chosen.");
			System.out.println("What would you like to do to the Ack packets?");
			System.out.println("(9):  change opcode");
			System.out.println("(10): change block number");
		}

		response = input.nextInt();

		return response;
	}

	// method to return the type of packet received
	public String getPacketType(byte[] data) {
		if (data[1] == 1 || data[1] == 2) {
			return "request";
		} else if (data[1] == 3) {
			return "data";
		} else if (data[1] == 4) {
			return "ack";
		}
		return "error";
	}

	public void forward() {
		// create byte array to hold packet to be received
		byte[] data = new byte[516];

		// create packet to receive data from client
		receivePacket = new DatagramPacket(data, data.length);

		try {
			// receive packet from client
			// receive() method blocks until datagram is received, data is now
			// populated with recievd packet
			System.out.println("Receiving...");
			receiveSocket.receive(receivePacket);
			clientAddress = receivePacket.getAddress();
			clientPort = receivePacket.getPort();
			System.out.println("Packet received from client");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		System.out.println("\n");

		String packetType = "";
		int whatDo = 0; // default action (do nothing)
		while (actionPerformed == false) {

			packetType = getPacketType(data);
			whatDo = getOperation();
			actionPerformed = true;
		}
		if (whatDo == 0) {
			// no nothing, simply forward packet thats been received
			System.out.println("Forwarding packet without altering it");
			try {
				sendPacket = new DatagramPacket(data, receivePacket.getLength(), InetAddress.getLocalHost(),
						server1Port);
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
						server1Port);
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
				sendPacket = new DatagramPacket(data, receivePacket.getLength(), InetAddress.getLocalHost(),
						server1Port);
				System.out.println("Forwardging packet to server on port " + sendPacket.getPort());
				sendReceiveSocket.send(sendPacket);
				System.out.println("Packet forwarded");
			} catch (IOException ioe) {
				ioe.printStackTrace();
				System.exit(1);
			}
		} else if (packetType.equals("request") && whatDo == 6) {
			System.out.println("option to change request packet's mode has been chosen");
			// takes the request packet and alters the filename
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
				sendPacket = new DatagramPacket(data, receivePacket.getLength(), InetAddress.getLocalHost(),
						server1Port);
				System.out.println("Forwardging packet to server on port " + sendPacket.getPort());
				sendReceiveSocket.send(sendPacket);
				System.out.println("Packet forwarded.");
			} catch (IOException ioe) {
				ioe.printStackTrace();
				System.exit(1);
			}
		} else if (packetType.equals("data") && whatDo == 7) {
			System.out.println("Changing data packets opcode...");
			data[0] = 9;
			data[1] = 9;
			System.out.println("OPCODE changed to: " + data[0] + data[1]);
			try {
				sendPacket = new DatagramPacket(data, receivePacket.getLength(), InetAddress.getLocalHost(),
						server1Port);
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
			data = new byte[100];
			receivePacket = new DatagramPacket(data, data.length);

			System.out.println("Receiving...");
			sendReceiveSocket.receive(receivePacket);
			System.out.println("Packet received from server on port " + receivePacket.getPort());

			// forward packet to client
			sendPacket = new DatagramPacket(data, receivePacket.getLength(), clientAddress, clientPort);
			System.out.println("Forwarding packet back to client...");
			sendReceiveSocket.send(sendPacket);
			System.out.println("Packet forwarded. \n");

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

	public String extractFileName(byte[] data, int dataLength) {
		int i = 1;
		StringBuilder sb = new StringBuilder();
		while (data[++i] != 0 && i < dataLength) {
			sb.append((char) data[i]);
		}

		return sb.toString();
	}

	public void disconnect() {
		sendReceiveSocket.close();
		receiveSocket.close();
	}

	public static void main(String[] args) {
		ErrorSimulator er1 = new ErrorSimulator();
		er1.run();

	}

}