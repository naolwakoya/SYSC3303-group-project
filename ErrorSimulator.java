
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ErrorSimulator{
	// instance variables
	private DatagramSocket sendReceiveSocket;
	private DatagramPacket receivePacket, sendPacket;

	private boolean isConnected = false;

	private boolean readFinished = false;
	private boolean writeFinished = false;

	private InetAddress clientAddress;
	private int clientPort;

	private int proxyPort = 23;
	private int server1Port = 69;

	private boolean requestReceived = false;

	private Scanner input;

	private void connect() {
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


	private void run() {
		//if no connection has been established, the connect method will run
		if (!isConnected) {
			connect();
		}
		while(true){
			int operation = getOperation();
			if(operation == 0){
				//branch for normal operation
				while(true){
					byte[] data;
					data = receiveFromClient();

					String packetType;
					String requestType = "";

					if(!requestReceived){
						requestType = getRequestType(data);
						requestReceived = true;
					}

					sendToServer(data);

					//check to see if transaction is finished after ack packet sent to server in read situation
					if(readFinished || writeFinished){
						server1Port = 69;
						System.out.println("Breaking Loop");
						break;
					}

					data = new byte[516];

					data = receiveFromServer();

					//check if transaction is finished after packet forwarded to server
					//last data packet has been received in a read request or write request
					packetType = getPacketType(data);
					if(packetType.equals("data") && receivePacket.getLength()< 516){
						readFinished = true;
						System.out.println("Last Packet received, terminating read Transaction. PacketLength: " + receivePacket.getLength() + " " + readFinished);
					}else if(packetType.equals("ack")){
						writeFinished = true;
						System.out.println("Last Packet received, terminating write transaction. PacketLength: " + receivePacket.getLength() + " " + writeFinished);
					}

					sendToClient(data);

				}

				readFinished = false;
				writeFinished = false;
			}else if(operation == 4){
				//branch for changing request packets opcode
				byte[] data;
				data = receiveFromClient();

				System.out.println("Altering opcode of request packet...");
				data[0] = 9;
				data[1] = 9;
				System.out.println("OPCODE changed to: " + data[0] + data[1]);

				sendToServer(data);

				data = receiveFromServer();

				sendToClient(data);

				server1Port = 69;

			} else if (operation == 5) {
				//branch for changing filename in the the request packet
				// takes the request packet and alters the filename
				byte[] data;

				data = receiveFromClient();

				System.out.println("Altering filename of request packet...");

				String fileName = extractFileName(data, data.length);

				System.out.println("Original filename: " + fileName);
				System.out.println("Changing fileName to: wrongFileName.txt");

				// create new byteArray with different fileName
				fileName = "randomFileName";
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

				sendToServer(msg);

				data = receiveFromServer();

				sendToClient(data);

				server1Port = 69;
			}else if(operation == 6){
				//branch for changing the mode in the request packet
				//takes the request packet and alters the mode

				byte[] data;

				data = receiveFromClient();

				System.out.println("Altering mode of request packet...");
				System.out.println("Changing mode to: wrongMode");

				// create new byteArray with different fileName
				String fileName = extractFileName(data, data.length);
				String mode = "wrongMode";
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

				sendToServer(msg);

				data = receiveFromServer();

				sendToClient(data);

				server1Port = 69;

			}else if(operation == 7){
				//branch for changing the opcode in the data packet

				System.out.println("Changing Data packet's opcode...");
				byte[] data;

				data = receiveFromClient();

				if(data[1] == 1) {
					//if data[1] equals to 1, then it is a read request
					//data packet will be received from the server
					sendToServer(data);

					data = receiveFromServer();

					System.out.println("Original opcode: " + data[0] + data[1]);

					//alter the data's opcode
					data[0] = 9;
					data[1] = 9;

					System.out.println("New changed to: " + data[0] + data[1]);

					sendToClient(data);

					server1Port = 69;

				}else if(data[1] == 2){
					//if data[1] equals to 2, then it is a write request
					//data packet will be received from the client
					//this branch will change the data packet sent from client and forward it to server
					sendToServer(data);

					data = receiveFromServer(); //server sends ack

					sendToClient(data); //forward ack to client

					data = receiveFromClient(); //receive data packet from client

					//alter the data's opcode to generate error
					data[0] = 9;
					data[1] = 9;

					sendToClient(data); //send data packet to server with altered opcode

					data = receiveFromServer(); //should receive an error packet

					sendToClient(data); //forward error packet back to client

					server1Port = 69;
				}

			}else if(operation == 8){
				//branch for changing the block number in the data packet

				System.out.println("Changing Data packet's block number...");
				byte[] data;

				data = receiveFromClient(); //receive packet from client, should be a request packet

				if(data[1] == 1) {
					//if data[1] equals to 1, then it is a read request
					//data packet will be received from the server
					sendToServer(data); //forward request packet to client

					data = receiveFromServer();  // should be the first data packet from server

					System.out.println("Original Block Number: " + data[2] + data[3]);

					//alter the data's block number
					data[2] = 9;
					data[3] = 9;

					System.out.println("New Block Number: " + data[2] + data[3]);

					sendToClient(data); //sending the data packet with the changed block number to client

					server1Port = 69;

				}else if(data[1] == 2){
					//if data[1] equals to 2, then it is a write request
					//data packet will be received from the client

					sendToServer(data);

					data = receiveFromServer(); //server sends ack for write request

					sendToClient(data); //forward ack to client

					data = receiveFromClient(); //receive data packet from client

					System.out.println("Original Block Number: " + data[2] + data[3]);

					//alter the data's opcode to generate error
					data[2] = 9;
					data[3] = 9;

					System.out.println("New Block Number changed to: " + data[2] + data[3]);

					sendToClient(data); //send data packet to server with altered opcode

					data = receiveFromServer(); //should receive an error packet

					sendToClient(data); //forward error packet back to client

					server1Port = 69;
				}

			}else if(operation == 9){
				//branch for changing the opcode in the ack packet

				System.out.println("Changing Ack packet's opcode...");
				byte[] data;


				data = receiveFromClient(); //receive packet from client, should be a request packet

				if(data[1] == 1){
					//read request, first ack packet will be send from client

					sendToServer(data); //forwarding request packet to server

					data = receiveFromServer();  //receive first data packet from server

					sendToClient(data); //forward first data packet back to client

					data = receiveFromClient(); //receive the first ack packet from the client

					System.out.println("Original Opcode of Ack packet: " + data[0] + data[1]);

					data[0] = 9;
					data[1] = 9;

					System.out.println("New Opcode of Ack packet: " + data[0] + data[1]);

					System.out.println("Sending ack packet with altered opcode to server");

					sendToServer(data);

					server1Port = 69;

				}else if(data[1] == 2){
					//write request, first ack packet will be sent by server

					sendToServer(data); //forwarding request packet to server

					data = receiveFromServer(); //receive first ack packet from server

					//change the ack packets opcode
					System.out.println("Original Opcode of Ack packet: " + data[0] + data[1]);

					data[0] = 9;
					data[1] = 9;

					System.out.println("New Opcode of Ack packet: " + data[0] + data[1]);

					sendToClient(data);

					server1Port = 69;

				}


			}else if(operation == 10){
				//branch for changing the block number in the ack packet

				System.out.println("Changing the ack packets block number");
				byte[] data;

				data = receiveFromClient(); // receive packet from client, should be request packet

				if(data[1] == 1){
					//read request, first ack packet will be sent by client

					sendToServer(data); //forward request packet to server

					data = receiveFromServer(); //receive first data packet from server

					sendToClient(data); //forward first data packet back to client

					data = receiveFromClient(); //receive the first ack packet from client

					System.out.println("Original block number of Ack packet: " + data[2] + data[3]);

					data[2] = 9;
					data[3] = 9;

					System.out.println("New block number of Ack packet: " + data[2] + data[3]);

					sendToServer(data);

					server1Port = 69;

				}

			}else if(operation == 11){
				//branch for delaying a request packet


			}else if(operation == 12){
				//branch for delaying a data packet
				
			}else if(operation == 13){
				//branch for delaying an ack packet

			}

		}
	}

	/*
	Asks the user what operation they would like to do to the packets.
	 */
	private int getOperation() {
		int response = 9;
		System.out.println("What would you like do?");
		System.out.println("(0): normal operation");
		System.out.println("(1): change request packets");
		System.out.println("(2): change data packets");
		System.out.println("(3): change ack packets");
		System.out.println("(4): Delay a packet");
		System.out.println("(5): Close the ErrorSimulator");

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
		}else if(response == 4){
			System.out.println("(4)Delay Packet chosen");
			System.out.println("Which packet would you like to delay?");
			System.out.println("(11): Request Packet");
			System.out.println("(12): Data packet");
			System.out.println("(13): Ack packet");
		}else if(response == 5){
			System.out.println("ErrorSimulator Closing.");
			System.out.println("Goodbye.");
			System.exit(1);
		}

		response = input.nextInt();

		return response;
	}

	// method to return the type of packet received
	private String getPacketType(byte[] data) {

		if (data[1] == 1 || data[1] == 2) {
			System.out.println("Pakcet type: Request");
			return "request";
		} else if (data[1] == 3) {
			System.out.println("Pakcet type: data");
			return "data";
		} else if (data[1] == 4) {
			System.out.println("Pakcet type: ack");
			return "ack";
		}
		return "error";
	}

	private String getRequestType(byte[] data){
		if (data[1] == 1 ) {
			return "read";
		} else if (data[1] == 2) {
			return "write";
		}

		return null;
	}

	//method to receive packet from client
	private byte[] receiveFromClient(){
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
	private void sendToServer(byte[] data){
		System.out.println("Forwarding packet...");
		try {
			sendPacket = new DatagramPacket(data, receivePacket.getLength(), InetAddress.getLocalHost(), server1Port);
			System.out.println("Forwarding packet to server on port " + sendPacket.getPort());
			sendReceiveSocket.send(sendPacket);
			System.out.println("Packet forwarded.");


		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.exit(1);
		}
	}

	private byte[] receiveFromServer(){
		System.out.println("\n");

		// receive response from server
		byte[] data = new byte[516];
		receivePacket = new DatagramPacket(data, data.length);
		System.out.println("Receiving from server...");

		try{
			sendReceiveSocket.receive(receivePacket);
			System.out.println("Packet received from server on port " + receivePacket.getPort());
			System.out.println("Packet size from server: " + receivePacket.getLength());
			server1Port = receivePacket.getPort();

		}catch(IOException e){
			e.printStackTrace();
		}

		return data;
	}

	private void sendToClient(byte[] data){
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