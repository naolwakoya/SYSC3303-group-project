import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class Client {
	
	static int REQUEST_PORT = 69;
	int sourceTID, destinationTID;
	
    DatagramSocket sendReceiveSocket;
    DatagramPacket receivePacket;
    
    boolean connected = false;
	
	public Client(){
		
	}
	
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
	
	public void writeFile(String fileName){
		try {
			String filePath = System.getProperty("user.dir") + "/" + fileName;
			System.out.println(filePath);
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
				try {
					sendReceiveSocket.send(dataPacket.generatePacket(InetAddress.getLocalHost(), destinationTID));
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
	
	public void readFile(String fileName){
		
	}
	
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
	}
	

	
	public boolean isConnected(){
		if (connected==true)
			return true;
		else 
			return false;
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
			
		}
		
	}

	
}
