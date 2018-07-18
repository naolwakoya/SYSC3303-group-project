import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.SyncFailedException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;

//import test.ErrorChecker;




public class Client {

	static int REQUEST_PORT = 8080;
	int sourceTID, destinationinationTID;

    DatagramSocket sendReceiveSocket;
    static DatagramPacket receivePacket;
	DatagramPacket sendPacket;
    private static Scanner input;
    public static final String fileDirectory = "files\\client\\";
	String filename = "something.txt";
    //ErrorChecker errorChecker1 = null;
    String mode = "octet";
    boolean connected = false;
    boolean verbose = true;
    //ErrorChecker errorChecker = null;

    public enum Opcode {
		RRQ ((byte)1), WRQ ((byte)2), DATA ((byte)3), ACK ((byte)4), ERROR ((byte)5);

		Opcode (byte op) {
		}

		public Object op() {
			// TODO Auto-generated method stub
			return null;
		}
	}


	public Client() {
		try {
			sendReceiveSocket = new DatagramSocket();

		}catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}






	 // Establishes a read or write connection with the server according to the TFTP protocol


	public void establishConnection(String fileName, String request){
        try {

            sendReceiveSocket = new DatagramSocket();
        }
        catch(SocketException se){
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


        this.receive();

        destinationinationTID = receivePacket.getPort();
        System.out.println("Connected to server");
        connected = true;
	}

	public void userInterface() {
		// determine if user wants to send a read request or a write request
		Opcode op;	// the user's choice of request to send
		input = new Scanner(System.in);		// scans user input
		while (true) {
			System.out.println("\nWould you like to make a (R)ead Request, (W)rite Request, or (Q)userInterfacet?");
			String choice = input.nextLine();	// user's choice
			if (choice.equalsIgnoreCase("R")) {			// read request
				op = Opcode.RRQ;
				System.out.println("\nClient: You have chosen to send a read request.");
				break;
			} else if (choice.equalsIgnoreCase("W")) {	// write request
				op = Opcode.WRQ;
				System.out.println("\nClient: You have chosen to send a write request.");
				break;
			} else if (choice.equalsIgnoreCase("Q")) {	// quserInterfacet
				System.out.println("\nGoodbye!");
				System.exit(0);
			} else {
				System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
			}
		}

		// determines where the user wants to send the request
		int destination; // the port destinationination of the user's request
		while (true) {
			System.out.println("Where would you like to send your request: ");


			String choice = input.nextLine();	// user's choice
			if (choice.equalsIgnoreCase("S")) {			// request to Server
				destination = 69;
				System.out.println("\nClient: You have chosen to send your request to the Server.");
				break;
			} else if (choice.equalsIgnoreCase("E")) {	// request to Error Simulator
				destination = 68;
				System.out.println("\nClient: You have chosen to send your request to the Error Simulator.");
				break;
			} else if (choice.equalsIgnoreCase("Q")) {	//
				System.out.println("\nGoodbye!");
				System.exit(0);
			} else {
				System.out.println("\nI'm sorry, that is not a valid choice.  Please try again");
			}
		}

		// determine which file the user wants to modify
		while(true) {
			System.out.println("Please choose a file to modify. Type in a file name: /n");
			filename = input.nextLine();	// user's choice

			// deal with user's choice of request
			if (op == Opcode.RRQ) {
				if (!(Files.exists(Paths.get(fileDirectory + filename)))) {	// file does not exist
					System.out.println("\nClient: You have chosen the file: " + filename +
							", to be received in " + mode + " mode.");
					break;
				} else{														// file already exists
					System.out.println("\nClient: I'm sorry, " + fileDirectory + filename + " already exists:");
					while(true) {
						System.out.println("(T)ry another file, or (Q)userInterfacet: ");
						String choice = input.nextLine();	// user's choice
						if (choice.equalsIgnoreCase("Q")) {			// quserInterfacet
							System.out.println("\nGoodbye!");
							System.exit(0);
						} else if (choice.equalsIgnoreCase("T")) {	// try another file
							break;
						} else {									// invalid choice
							System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
						}
					}
				}
			} else if (op == Opcode.WRQ) {
				if (Files.isWritable(Paths.get(fileDirectory + filename))) {
					System.out.println("\nClient: You have chosen the file: " + fileDirectory + filename + ", to be sent in " +
							mode + " mode.");
					break;
				} else {														// file does not exist
					System.out.println("\nClient: I'm sorry, " + fileDirectory + filename + " does not exist:");
					while(true) {
						System.out.println("(T)ry another file, or (Q)userInterfacet: ");
						String choice = input.nextLine();	// user's choice
						if (choice.equalsIgnoreCase("Q")) {			// quserInterfacet
							System.out.println("\nGoodbye!");
							System.exit(0);
						} else if (choice.equalsIgnoreCase("T")) {	// try another file
							break;
						} else {									// invalid choice
							System.out.println("\nI'm sorry, that is not a valid choice.  Please try again...");
						}
					}
				}
			}
		}

		byte[] request = createRequest(op.op(), filename, mode);	// get the request byte[] to send


		try{
			send(request, InetAddress.getLocalHost(), destination);
		} catch (UnknownHostException e) {
			System.out.println("\nClient: Error, InetAddress could not be found. Shutting Down...");
			System.exit(1);
		}
	}

	public static byte[] createRequest(Object object, String filename, String mode) {
		byte data[]=new byte[filename.length() + mode.length() + 4];
		Thread thread;

		// request opcode
		data[0] = 0;
		data[1] = (byte) object;


		// convert filename and mode to byte[], with proper encoding
		byte[] fn = null;	// filename
		byte[] md = null;	// mode
		try {
			fn = filename.getBytes("US-ASCII");
			md = mode.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		// add filename and mode to request
		data[fn.length + 3] = 0;
		System.arraycopy(fn,0,data,2,fn.length);
		System.arraycopy(md,0,data,fn.length+3,md.length);
		data[data.length-1] = 0;

		thread = new Thread(new TftpClientConnectionThread(receivePacket));
        thread.start();

		return data;
	}

	/*
	 * Writes a file to the server
	 */
	public void writeFile(String fileName){
		try {
			String filePath = System.getProperty("user.dir") + "/clientFiles/" + fileName;
			//Make sure file exists
			File file = new File(filePath);
			if (!file.exists()) {
				System.out.println("Cannot find the file: " + fileName);
				return;
			}

			FileInputStream inputStream = new FileInputStream(file);

			int blockNumber = 1;
			int nRead=0;
			byte[] data = new byte [500];
			TftpData dataPacket;

			do {
				nRead = inputStream.read(data);
				if (nRead == -1) {
					nRead = 0;
					data = new byte[0];
				}
				dataPacket = new TftpData(blockNumber, data, nRead);
				sendPacket = dataPacket.generatePacket(receivePacket.getAddress(), destinationinationTID);

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


			} while (nRead == 500);

			inputStream.close();


		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/*
	 * Reads a file from the server
	 */
	public void readFile(String fileName){
		String filePath = System.getProperty("user.dir") + "/clientFiles/" + fileName;
		try{
			File file = new File(filePath);
			if (file.exists() && !file.canWrite()){
				System.out.println("Can't overwrite existing file");
				return;
			}

			byte[] fileData;
			FileOutputStream outputStream = new FileOutputStream(filePath);
			int blockNumber = 1;

			do {
				try{
					this.receive();

					if (file.canWrite()){
						fileData = extractFromDataPacket(receivePacket.getData(), receivePacket.getLength());
						outputStream.write(fileData);
						outputStream.getFD().sync();
					} else{
						System.out.println("Cannot write to file");
						return;
					}
	            	// Send acknowledgement packet
	            	TftpAck ack = new TftpAck(blockNumber++);

					sendPacket = ack.generatePacket(receivePacket.getAddress(), destinationinationTID);

			        if (verbose) {
			            System.out.println("Sending packet:");
			            printPacketInformation(sendPacket);
			            }

	            	try {
	            		sendReceiveSocket.send(sendPacket);
	            	}catch (IOException e) {
	                    e.printStackTrace();
	                    System.exit(1);
	                }

				}
				catch (SyncFailedException e){
					outputStream.close();
					file.delete();
					return;
				}
			} while (!(fileData.length<500));

			outputStream.close();
    	}
    	catch (FileNotFoundException e1){
    		return;
    	}catch (IOException e) {
    		new File(filePath).delete();
			return;
    	}
    }

	/*
	 * Waits to receive a packet froim the sendReceiveSocket
	 */
	public DatagramPacket receive(){
		 //Create a DatagramPacket for receiving packets
        byte receive[] = new byte[1024];
        receivePacket = new DatagramPacket(receive, receive.length);
        Thread thread;

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
        thread = new Thread(new TftpClientConnectionThread(receivePacket));
        thread.start();
		return receivePacket;
	}


	/*
	 * Processes the received Datagram and Prints the packet information into the console
	 */
	public void printPacketInformation(DatagramPacket packet) {
        System.out.println("Host: " + packet.getAddress());
        System.out.println("Host port: " + packet.getPort());
        System.out.println("Packet length: " + packet.getLength());
        System.out.println("Containing: " + packet.getData());
        String packetString = new String(packet.getData(),0,packet.getLength());
        System.out.println("String form: " + packetString + "\n");
	}


	public void send (byte[] data, InetAddress addr, int port) {
		sendPacket = new DatagramPacket(data, data.length, addr, port);

		System.out.println("\nClient: Sending packet:");
		System.out.println("To host: " + sendPacket.getAddress() + " : " + sendPacket.getPort());
		System.out.print("Containing " + sendPacket.getLength() + " bytes: \n");
		System.out.println(Arrays.toString(data) + "\n");

		// send the DatagramPacket to the server via the send/receive socket
		try {
			sendReceiveSocket.send(sendPacket);
			System.out.println("Client: Packet sent");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}






	public boolean isConnected(){
		if (connected==true)
			return true;
		else
			return false;
	}



	public class TftpAck extends TftpPacket{

		int blockNumber = 0;

		public TftpAck(int blockNumber){
			this.blockNumber = blockNumber;
		}

		public int getBlockNumber(){
			return blockNumber;
		}

		@Override
		public byte[] generateData() {
			byte[] data = new byte[4];
			data[0] = 0;
			data[1] = (byte) 4;
			data[2] = (byte) (blockNumber >> 8);
			data[3] = (byte) (blockNumber);
			return data;
		}

	}





	// TODO Auto-generated method stub





/*

	private byte[] createData(byte blockNumber, byte[] fileData) {
		// TODO Auto-generated method stub
		return null;
	}





	private byte[] processDatagram(DatagramPacket datagram) {
		return null;
	}
	*/





	/*
	 * returns true if in verbose mode and false if in quserInterfaceet mode
	 */
	public boolean getMode() {
		return verbose;
	}

	/*
	 * Toggles between quserInterfaceet mode and verbose mode
	 */
	public void toggleMode() {
		if (verbose)
			verbose = false;
		else
			verbose = true;
	}

	public byte[] createError (byte errorCode, String errorMsg) {
		byte[] error = new byte[4 + errorMsg.length() + 1];	// new error to eventually be sent to server

		// add opcode
		error[0] = 0;
		error[1] = 5;

		// add error code
		error[2] = 0;
		error[3] = errorCode;

		byte[] message = new byte[errorMsg.length()];	// new array for errorMsg, to be joined with codes

		// convert errorMsg to byte[], with proper encoding
		try {
			message = errorMsg.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		// add error message to error byte[]
		System.arraycopy(message, 0, error, 4, message.length);
		error[error.length-1] = 0;	// make last element a 0 byte, according to TFTP

		return error; //return full error message with opcodes and type of error
	}

	public void parseAck (byte[] ack) {
		System.out.println("\nClient: Recieved packet is ACK: ");
		System.out.println("Block#: " + ack[2] + ack[3]);
	}

	public byte[] parseData (byte[] data) {
		// byte[] for the data portion of DATA packet byte[]
		byte[] justData = new byte[data.length - 4];
		System.arraycopy(data, 4, justData, 0, data.length-4);

		// print info to user
		System.out.println("\nClient: Recieved packet is DATA: ");
		System.out.println("Block#: " + data[2] + data[3] + ", and containing data: ");
		System.out.println(Arrays.toString(justData));

		return justData;
	}



	public void parseError (byte[] error) {
		System.out.println("\nClient: Recieved packet is ERROR: ");

		// get the error message
		byte[] errorMsg = new byte[error.length - 5];
		System.arraycopy(error, 4, errorMsg , 0, error.length - 5);
		String message = null;
		try {
			message = new String(errorMsg, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		// display error code to user
		byte errorCode = error[3];	// get error code
		if (errorCode == 0) {
			System.out.println("Error Code: 00: Not defined, see error message (if any). ");
		} else if (errorCode == 1) {
			System.out.println("Error Code: 01: File not found. ");
		} else if (errorCode == 2) {
			System.out.println("Error Code: 02: Access violation. ");
		} else if (errorCode == 3) {
			System.out.println("Error Code: 03: Disk full or allocation exceeded. ");
		} else if (errorCode == 6) {
			System.out.println("Error Code: 06: File already exists. ");
		} else {
			System.out.println("Error Code: " + errorCode);
		}

		// display error message to user
		System.out.println("Error message:" + message);
	}

	/*
	 * returns the byte array of the data in the tftp data packet
	 */
    public byte[] extractFromDataPacket(byte[] data, int dataLength){
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(data, 4, dataLength-4);
		return data = stream.toByteArray();
    }

	public static void main(String[] args) throws Exception{
		Client c = new Client();
		System.out.println("Welcome to SYSC3303-groupproject TFTP Client \n");
		Scanner s = new Scanner(System.in);
		String input;

		while (true){
			System.out.println("Enter a command:");
			input = s.nextLine().toLowerCase();
			String[] cmd = input.split("\\s+");

			if (cmd.length==0){
				continue;
			}
			else if (cmd[0].equals("stop")){
				s.close();
				return;
			}
			else if ((cmd[0].equals("read") || cmd[0].equals("write")) && cmd[1].length()>0){
				c.establishConnection(cmd[1],cmd[0]);

				if (c.isConnected()){
					if (cmd[0].equals("read")){
						c.readFile(cmd[1]);
					}
					else
						c.writeFile(cmd[1]);
				}
				else{
					System.out.println("Unable to connect to server");
				}

			}
			else if (cmd[0].equals("mode")) {
				c.toggleMode();
				if (c.getMode())
					System.out.println("Client is now in verbose mode");
				else
					System.out.println("Client is now in quserInterfaceet mode");
			}
			//c.connection();

		}

	}


}
