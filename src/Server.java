import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.SyncFailedException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Server {

    DatagramSocket sendReceiveSocket, receiveSocket;
    DatagramPacket sendPacket, receivePacket;
    String fileName;

    public Server(){
        try {
            //Create a datagram socket for sending packets
            sendReceiveSocket = new DatagramSocket();
            //Create a datagram socket for receiving packets on port 69
            receiveSocket = new DatagramSocket(69);
        }
        catch(SocketException se){
            se.printStackTrace();
            System.exit(1);
        }
    }

    public void run (){
    	
        while(!receiveSocket.isClosed()){

            //Create a DatagramPacket for receiving packets
            byte data[] = new byte[1024];
            receivePacket = new DatagramPacket(data, data.length);

            // Block until a datagram packet is received from the receive socket
            try{
                System.out.println("Waiting...");
                receiveSocket.receive(receivePacket);
            } catch(IOException e){
                System.out.print("IO Exception: likely:");
                System.out.println("Receive Socket Timed Out.\n" + e);
                e.printStackTrace();
                System.exit(1);
            }
            
            // Check if it is a write request
            if (data[1]==2)
            {
            	fileName = extractFileName(data,data.length);
            	
            	// Send acknowledgement packet
            	TftpAck ack = new TftpAck(0);
            	try {
            		sendReceiveSocket.send(ack.generatePacket(receivePacket.getAddress(), receivePacket.getPort()));
            	}catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            	
            	receiveFile();
            }
            // Check if it is a read request
            else if (data[1]==1){
            	
            	fileName = extractFileName(data,data.length);
            	
            	// Send empty data backet with block number 1
            	TftpData dat = new TftpData(1,null,0);
            	try {
            		sendReceiveSocket.send(dat.generatePacket(receivePacket.getAddress(), receivePacket.getPort()));
            	}catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            	
            }

        }
        //Close the sockets once complete
        receiveSocket.close();
        sendReceiveSocket.close();
    }

    
    public void receiveFile(){
    	try {
    		String filePath = System.getProperty("user.dir") + "/server/" + fileName;
			// Check that file does not exist already
    		File file = new File(filePath);
			if (file.exists()) {
				System.out.println("The file already exists!");
				return;
			}
			byte[] fileData;
			FileOutputStream outputStream = new FileOutputStream(file);
			int blockNumber = 1;
			do {
				try{
					this.receive();
					
					if (file.canWrite()){
						fileData = extractFromDataPacket(receivePacket.getData(), receivePacket.getLength());
						outputStream.write(fileData);
						outputStream.getFD().sync();
						System.out.println(fileData.length);
					} else{
						System.out.println("Cannot write to file");
						return;
					}
	            	// Send acknowledgement packet
	            	TftpAck ack = new TftpAck(blockNumber++);
	            	try {
	            		sendReceiveSocket.send(ack.generatePacket(receivePacket.getAddress(), receivePacket.getPort()));
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
			return;
    	}
    }
    
    public void sendFile(){
    	
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
       
       //Process the received datagram
       System.out.println("Received packet:");
       System.out.println("From host: " + receivePacket.getAddress());
       System.out.println("Host port: " + receivePacket.getPort());
       System.out.println("Packet length: " + receivePacket.getLength());
       System.out.println("Containing: " + receivePacket.getData());
       String received = new String(receivePacket.getData(),0,receivePacket.getLength());
       System.out.println("String form: " + received + "\n");
	}
    
    
    public byte[] extractFromDataPacket(byte[] data, int dataLength){
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(data, 4, dataLength-4);
		return data = stream.toByteArray();
    }
    
    public String extractFileName(byte[] data, int dataLength){
    	int i = 1;
    	StringBuilder sb = new StringBuilder();
    	while(data[++i] != 0 && i < dataLength){
    		sb.append((char)data[i]);
    	}
    	return sb.toString();
    }


    public static void main (String[] args) throws Exception{
    	Server s = new Server();
    	s.run();
    }

}
