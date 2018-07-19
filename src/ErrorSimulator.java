import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ErrorSimulator {

	public static int DEFAULT_PORT = 69;
    DatagramSocket receiveSocket, sendReceiveSocket;
    DatagramPacket receivePacket, sendPacket, sendReceivePacket;

    public ErrorSimulator(){
        try {
            // Creates a datagram socket for sending and receiving packets
            sendReceiveSocket = new DatagramSocket();

            // Creates a datagram socket and binds it to port 20 for receiving packets
            receiveSocket = new DatagramSocket(23);
        }
        catch(SocketException se){
            se.printStackTrace();;
            System.exit(1);
        }
    }

    public void run () {
        boolean running = true;

        while (running) {
            // Construct a DatagramPacket for receiving packets up
            byte data[] = new byte[1024];
            receivePacket = new DatagramPacket(data, data.length);

            // Block until a datagram packet is received from receiveSocket.
            try {
                System.out.println("Waiting...");
                receiveSocket.receive(receivePacket);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }

            // Process the received datagram.
            System.out.println("Proxy: Packet received:");
            System.out.println("From host: " + receivePacket.getAddress());
            System.out.println("Host port: " + receivePacket.getPort());
            System.out.println("Packet length: " + receivePacket.getLength());
            System.out.println("Containing: " + receivePacket.getData());
            // Form a String from the byte array.
            String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("String form: " + received + "\n");

            // Check to see if the packet is a read or write request
            if (data[1]==2 || data[1]==1){
	            // Create a new datagram packet containing the string received from the client
	            try {
	                sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
	                        InetAddress.getLocalHost(), DEFAULT_PORT);
	            } catch (UnknownHostException e) {
	                e.printStackTrace();
	                System.exit(1);
	            }
	
	            // Output the packet information
	            System.out.println("Proxy: Sending packet:");
	            System.out.println("To host: " + sendPacket.getAddress());
	            System.out.println("Destination host port: " + sendPacket.getPort());
	            System.out.println("Packet length: " + sendPacket.getLength());
	            System.out.println("Containing: " + sendPacket.getData());
	            System.out.println("String form: " + new String(sendPacket.getData(), 0, sendPacket.getLength()));
	
	            // Send the datagram packet to the server via the sendReceive socket
	            try {
	                sendReceiveSocket.send(sendPacket);
	            } catch (IOException e) {
	                e.printStackTrace();
	                System.exit(1);
	            }
	            System.out.println("Proxy: packet sent" + "\n");
	
	            // Create a DatagramPacket for receiving the response from the server
	            byte response[] = new byte[100];
	            sendReceivePacket = new DatagramPacket(response, response.length);
	
	            //Block until the datagram packet is received from the sendReceiveSocket
	            try {
	                System.out.println("Waiting for response..."); // so we know we're waiting
	                sendReceiveSocket.receive(sendReceivePacket);
	            } catch (IOException e) {
	                System.out.print("IO Exception: likely:");
	                System.out.println("Receive Socket Timed Out.\n" + e);
	                e.printStackTrace();
	                System.exit(1);
	            }
	
	            //Process the received response datagram
	            System.out.println("Proxy: Packet response received:");
	            System.out.println("From host: " + sendReceivePacket.getAddress());
	            System.out.println("Host port: " + sendReceivePacket.getPort());
	            System.out.println("Packet length: " + sendReceivePacket.getLength());
	            System.out.println("Containing: " +  sendReceivePacket.getData());
	            // Form a String from the byte array.
	            received = new String(sendReceivePacket.getData(),0,sendReceivePacket.getLength());
	            System.out.println("String form: " + received + "\n");
	
	            //Form a new packet to send back to the client
	            sendPacket = new DatagramPacket(sendReceivePacket.getData(), sendReceivePacket.getLength(),
	                    receivePacket.getAddress(), receivePacket.getPort());
	
	
	            //Output all the information about the packet
	            System.out.println( "Proxy: Sending packet:");
	            System.out.println("To host: " + sendPacket.getAddress());
	            System.out.println("Destination host port: " + sendPacket.getPort());
	            System.out.println("Packet length: " + sendPacket.getLength());
	            System.out.println("Containing: " + sendPacket.getData());
	            System.out.println("String form: " + new String(sendPacket.getData(),0,sendPacket.getLength()));
	
	            // Send the datagram packet to the client via the send socket.
	            try {
	                sendReceiveSocket.send(sendPacket);
	            } catch (IOException e) {
	                e.printStackTrace();
	                System.exit(1);
	            }
	
	            System.out.println("Proxy: packet sent" + "\n");
	        }
            else {
	            // Create a new datagram packet containing the string received from the client
	            try {
	                sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
	                        InetAddress.getLocalHost(), sendReceivePacket.getPort());
	            } catch (UnknownHostException e) {
	                e.printStackTrace();
	                System.exit(1);
	            }
	
	            // Output the packet information
	            System.out.println("Proxy: Sending packet:");
	            System.out.println("To host: " + sendPacket.getAddress());
	            System.out.println("Destination host port: " + sendPacket.getPort());
	            System.out.println("Packet length: " + sendPacket.getLength());
	            System.out.println("Containing: " + sendPacket.getData());
	            System.out.println("String form: " + new String(sendPacket.getData(), 0, sendPacket.getLength()));
	
	            // Send the datagram packet to the server via the sendReceive socket
	            try {
	                sendReceiveSocket.send(sendPacket);
	            } catch (IOException e) {
	                e.printStackTrace();
	                System.exit(1);
	            }
	            System.out.println("Proxy: packet sent" + "\n");
	
	            // Create a DatagramPacket for receiving the response from the server
	            byte response[] = new byte[100];
	            sendReceivePacket = new DatagramPacket(response, response.length);
	
	            //Block until the datagram packet is received from the sendReceiveSocket
	            try {
	                System.out.println("Waiting for response..."); // so we know we're waiting
	                sendReceiveSocket.receive(sendReceivePacket);
	            } catch (IOException e) {
	                System.out.print("IO Exception: likely:");
	                System.out.println("Receive Socket Timed Out.\n" + e);
	                e.printStackTrace();
	                System.exit(1);
	            }
	
	            //Process the received response datagram
	            System.out.println("Proxy: Packet response received:");
	            System.out.println("From host: " + sendReceivePacket.getAddress());
	            System.out.println("Host port: " + sendReceivePacket.getPort());
	            System.out.println("Packet length: " + sendReceivePacket.getLength());
	            System.out.println("Containing: " +  sendReceivePacket.getData());
	            // Form a String from the byte array.
	            received = new String(sendReceivePacket.getData(),0,sendReceivePacket.getLength());
	            System.out.println("String form: " + received + "\n");
	
	            //Form a new packet to send back to the client
	            sendPacket = new DatagramPacket(sendReceivePacket.getData(), sendReceivePacket.getLength(),
	                    receivePacket.getAddress(), receivePacket.getPort());
	
	
	            //Output all the information about the packet
	            System.out.println( "Proxy: Sending packet:");
	            System.out.println("To host: " + sendPacket.getAddress());
	            System.out.println("Destination host port: " + sendPacket.getPort());
	            System.out.println("Packet length: " + sendPacket.getLength());
	            System.out.println("Containing: " + sendPacket.getData());
	            System.out.println("String form: " + new String(sendPacket.getData(),0,sendPacket.getLength()));
	
	            // Send the datagram packet to the client via the send socket.
	            try {
	                sendReceiveSocket.send(sendPacket);
	            } catch (IOException e) {
	                e.printStackTrace();
	                System.exit(1);
	            }
	
	            System.out.println("Proxy: packet sent" + "\n");   
            }
        }

        sendReceiveSocket.close();
        receiveSocket.close();
    }

    public static void main( String args[] )
    {
        ErrorSimulator e = new ErrorSimulator();
        e.run();
    }
	
	
}
