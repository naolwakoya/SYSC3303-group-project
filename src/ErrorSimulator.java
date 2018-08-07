
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ErrorSimulator{
	// instance variables
	private DatagramSocket sendReceiveSocket;
	private DatagramPacket receivePacket, sendPacket;

	boolean isConnected = false;

	boolean readFinished = false;
	boolean writeFinished = false;

	InetAddress clientAddress;
	int clientPort;

	int proxyPort = 23;
	int server1Port = 69;

	boolean actionPerformed = false;

	Scanner input;

	public void connect() {
		// scanner to receive user input from prompts
		input = new Scanner(System.in);

		// create new datagram sockets for the client and server
		try {
			sendReceiveSocket = new DatagramSocket(proxyPort, InetAddress.getLocalHost());
			System.out.println("Connected to client on port: " + sendReceiveSocket.getLocalPort());
		} catch (IOException se) {
			se.printStackTrace();
			System.exit(1);
		}
		isConnected = true;
	}

	public void run() {
		//if no connection has been established, the connect method will run
		if (isConnected == false) {
			connect();
		}
		while(true){
			while(true){

				byte[] data;
				data = receiveClient();

				sendServer(data);

				//check to see if transaction is finished after ack packet sent to server in read situation
				if(readFinished == true || writeFinished == true){
					server1Port = 69;
					break;
				}

				data = new byte[516];


				data = receiveServer(data);

				//check if transaction is finished after packet forwarded to server
				//last data packet has been received in a read request or write request
				String packetType = getPacketType(data);
				if(packetType == "data" && receivePacket.getLength()< 516){
					readFinished = true;
					System.out.println("LastPacket received, PacketLength: " + receivePacket.getLength() + " " + readFinished);
				}else if(packetType == "ack"){
					writeFinished = true;
					System.out.println("LastPacket received, PacketLength: " + receivePacket.getLength() + " " + writeFinished);
				}

				sendClient(data);

			}

			readFinished = false;
			writeFinished = false;

		}
	}

	// asks the user what type of operation to perform
	public int getOperation() {
		int response = 9;
		System.out.println("What would you like change?");
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

	//method to receive packet from client
	private byte[] receiveClient(){
		// create byte array to hold packet to be received
		byte[] data = new byte[516];

		// create packet to receive data from client
		receivePacket = new DatagramPacket(data, data.length);

		try {
			// receive packet from client
			// receive() method blocks until datagram is received, data is now
			// populated with recievd packet
			System.out.println("Receiving from Client...");
			sendReceiveSocket.receive(receivePacket);
			clientAddress = receivePacket.getAddress();
			clientPort = receivePacket.getPort();

			System.out.println("Packet received from client");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return data;
	}

	//method to receive packet from server
	private void sendServer(byte[] data){
		System.out.println("Forwarding packet without altering it");
		try {
			sendPacket = new DatagramPacket(data, receivePacket.getLength(), InetAddress.getLocalHost(), server1Port);
			System.out.println("Forwarding packet to server on port " + sendPacket.getPort());
			sendReceiveSocket.send(sendPacket);
			System.out.println("Packet forwarded.");

			//if transaction is equaled to false, last ack packet from client has been send to server, no need
			// to wait for response from server, exit the loop

		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.exit(1);
		}
	}

	private byte[] receiveServer(byte[] data){
		System.out.println("\n");

		// receive response from server
		data = new byte[516];
		receivePacket = new DatagramPacket(data, data.length);
		System.out.println("Receiving from server...");

		try{
			sendReceiveSocket.receive(receivePacket);
			System.out.println("Packet received from server on port " + receivePacket.getPort());
			System.out.println("Packer size from server: " + receivePacket.getLength());
			server1Port = receivePacket.getPort();

		}catch(IOException e){
			e.printStackTrace();
		}

		return data;
	}

	private void sendClient(byte[] data){
		// forward packet to client
		try{
			sendPacket = new DatagramPacket(data, receivePacket.getLength(), clientAddress, clientPort);
			System.out.println("Forwarding packet back to client...");
			sendReceiveSocket.send(sendPacket);
			System.out.println("Packet forwarded. \n");
		}catch(IOException e){
			e.printStackTrace();
		}
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
		ErrorSimulator er1 = new ErrorSimulator();
		er1.run();

	}

}