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
	
	static int REQUEST_PORT = 69;
	int sourceTID, destinationTID;
	
    DatagramSocket sendReceiveSocket;
    DatagramPacket receivePacket, sendPacket;
    
    boolean connected = false;
    boolean verbose = true;
	
	public Client(){
		
	}
	
	/*
	 * Establishes a read or write connection with the server
	 * according to the TFTP protocol
	 */
	public void establishConnection(String fileName, String request){
        try {
            // Create a datagram socket for sending and receiving packets
            sendReceiveSocket = new DatagramSocket();
        }
        catch(SocketException se){
            se.printStackTrace();
            System.exit(1);
        }
        
        TftpRequest req = new TftpRequest(fileName, request);
        
        // Send the packet via the sendReceiveSocket
        try {
            sendReceiveSocket.send(req.generatePacket(InetAddress.getLocalHost(), REQUEST_PORT));
            sourceTID = sendReceiveSocket.getPort();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        
        this.receive();
        
        destinationTID = receivePacket.getPort();
        System.out.println("Connected to server");
        connected = true;
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
				System.out.println("Cannot find file: " + fileName);
				return;
			}

			FileInputStream inputStream = new FileInputStream(file);
			
			int blockNumber = 1;
			int nRead=0;
			byte[] data = new byte [512];
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
					  System.out.println( "Sending packet:");
			            System.out.println("To host: " + sendPacket.getAddress());
			            System.out.println("Destination host port: " + sendPacket.getPort());
			            System.out.println("Packet length: " + sendPacket.getLength());
			            System.out.println("Containing: " + sendPacket.getData());
			            System.out.println("String form: " + new String(sendPacket.getData(),0,sendPacket.getLength()));
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
	            	
					sendPacket = ack.generatePacket(receivePacket.getAddress(), destinationTID);
					
					if (verbose) {
						  System.out.println( "Sending packet:");
				            System.out.println("To host: " + sendPacket.getAddress());
				            System.out.println("Destination host port: " + sendPacket.getPort());
				            System.out.println("Packet length: " + sendPacket.getLength());
				            System.out.println("Containing: " + sendPacket.getData());
				            System.out.println("String form: " + new String(sendPacket.getData(),0,sendPacket.getLength()) + "\n");
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
			} while (!(fileData.length<512));
			
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
	public void receive(){
		 //Create a DatagramPacket for receiving packets
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
        //Process the received datagram
        System.out.println("Received packet:");
        System.out.println("From host: " + receivePacket.getAddress());
        System.out.println("Host port: " + receivePacket.getPort());
        System.out.println("Packet length: " + receivePacket.getLength());
        System.out.println("Containing: " + receivePacket.getData());
        String received = new String(receivePacket.getData(),0,receivePacket.getLength());
        System.out.println("String form: " + received + "\n");
        }
	}
	

	/*
	 * returns true if the client has established a connection to the server
	 */
	public boolean isConnected(){
		if (connected==true)
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
	
	/*
	 * returns the byte array of the data in the tftp data packet
	 */
    public byte[] extractFromDataPacket(byte[] data, int dataLength){
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(data, 4, dataLength-4);
		return data = stream.toByteArray();
    }
	
	public static void main(String[] args){
		Client c = new Client();
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
					System.out.println("Client is now in quiet mode");
			}
			
		}
		
	}

	
}
