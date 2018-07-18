import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class TftpServer {



    private DatagramSocket  serverSocket, sendReceiveSocket;
    String fileName;
    public boolean serverOn;
    private boolean isReadRequest;

    public TftpServer(){
        serverOn=true;
        try {
            System.out.println("SERVER IS Instantiated");

            //Create a datagram socket for receiving packets on port 69
            serverSocket = new DatagramSocket(69);

        }
        catch(SocketException se){
            se.printStackTrace();
            System.exit(1);
        }
    }

    public void startReceiving() {
        DatagramPacket receivePacket=null;
        Thread thread;

        System.out.println("Starting server");

        while (serverOn) {

            try{
                //Create a DatagramPacket for receiving packets
                byte data[] = new byte[1024];
                receivePacket = new DatagramPacket(data, data.length);
                System.out.println("Waiting...");
                serverSocket.receive(receivePacket);
                
                sendReceiveSocket =  new DatagramSocket(); 
                
                if (data[1]==2)
                {
                    System.out.println("Received a write request");
                    isReadRequest = false;
                    fileName = extractFileName(data,data.length);

                    // Send acknowledgement packet
                    TftpAck ack = new TftpAck(0);
                    try {
                        sendReceiveSocket.send(ack.generatePacket(receivePacket.getAddress(), receivePacket.getPort()));
                    }catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
                // Check if it is a read request
                else if (data[1]==1){
                    System.out.println("Received a read request");
                    isReadRequest = true;
                    fileName = extractFileName(data,data.length);

                    // Send empty data packet with block number 1
                    TftpData dat = new TftpData(1,null,0);
                    try {
                        sendReceiveSocket.send(dat.generatePacket(receivePacket.getAddress(), receivePacket.getPort()));
                    }catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            } catch (SocketTimeoutException e) {
                continue;
            } catch (SocketException e) {
                continue;
            } catch (IOException e) {
                System.out.print("IO Exception: likely:");
                System.out.println("Receive Socket Timed Out.\n" + e);
                e.printStackTrace();
                System.exit(1);
            }

            thread = new Thread(new TftpClientConnectionThread(sendReceiveSocket, isReadRequest, fileName, receivePacket.getPort(), receivePacket.getAddress()));
            thread.start();

        }
    }
        
        /*
         * returns the filename from the request packet
         */
        public String extractFileName(byte[] data, int dataLength){
            int i = 1;
            StringBuilder sb = new StringBuilder();
            while(data[++i] != 0 && i < dataLength){
                sb.append((char)data[i]);
            }
            return sb.toString();
        }
        
	        
	    /*
	     *returns the server socket
	     */
	    public DatagramSocket getServerSocket() {
	        return serverSocket;
	    }
	    
	    public static void main(String[] args){
	
	
	        TftpServer server=new TftpServer();
	
	        Thread controlThread= new Thread(new TftpServerControl(server));
	
	        controlThread.start();
	
	        server.startReceiving();
	
	    }




}
