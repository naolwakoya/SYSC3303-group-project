
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class TftpClientConnectionThread implements Runnable {
    DatagramSocket sendReceiveSocket;
    DatagramPacket sendPacket, receivePacket;
    String fileName;
    boolean isReadRequest;
    InetAddress destinationAddress;
    int port;
    

    public TftpClientConnectionThread(boolean isReadRequest, DatagramPacket receivePacket){

    	try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
    	this.receivePacket = receivePacket;
    	this.isReadRequest = isReadRequest;
    	destinationAddress = receivePacket.getAddress();
    	port = receivePacket.getPort();

    }

    @Override
    public void run (){
        System.out.println("its in the RUN");
        if (isReadRequest){
        	fileName = extractFileName(receivePacket.getData(),receivePacket.getData().length);
       		sendFile();
        }
       	else{
            fileName = extractFileName(receivePacket.getData(), receivePacket.getData().length);
       		receiveFile();
       	}
        //Close the sockets once complete
        sendReceiveSocket.close();
    }


    public void receiveFile(){
        try {
            // Send acknowledgement packet
            TftpAck ack = new TftpAck(0);
            try {
                sendReceiveSocket.send(ack.generatePacket(destinationAddress, port));
            }catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        	
            String filePath = System.getProperty("user.dir") + "/serverFiles/" + fileName;
            // Check that file does not exist already
            System.out.println(filePath);
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
                    } else{
                        System.out.println("Cannot write to file");
                        return;
                    }
                    // Send acknowledgement packet
                    ack = new TftpAck(blockNumber++);
                    try {
                        sendReceiveSocket.send(ack.generatePacket(destinationAddress, port));
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

    /*
     * Sends the file to the client via tftp data packets
     */
    public void sendFile(){
        try {
            String filePath = System.getProperty("user.dir") + "/serverFiles/" + fileName;
            System.out.println(filePath);
            //Make sure file exists
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("Cannot find file: " + fileName);
                return;
            }
            if (!file.isAbsolute()) {
            	try {
            		TftpError error = new TftpError(2, "Cant access file in folder");
            		sendReceiveSocket.send(error.generatePacket(destinationAddress, port));
                	return;
            	}
            	catch (IOException e1){
            		e1.printStackTrace();
            	}
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
                    sendReceiveSocket.send(dataPacket.generatePacket(destinationAddress, port));
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
     * Waits to receive a packet from the sendReceive socket
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

        //Process the received datagram
        System.out.println("Received packet:");
        System.out.println("From host: " + receivePacket.getAddress());
        System.out.println("Host port: " + receivePacket.getPort());
        System.out.println("Packet length: " + receivePacket.getLength());
        System.out.println("Containing: " + receivePacket.getData().toString());
        String received = new String(receivePacket.getData(),0,receivePacket.getLength());
        System.out.println("String form: " + received + "\n");
    }
    /*
     * returns the byte array of the data in the tftp data packet
     */
    public byte[] extractFromDataPacket(byte[] data, int dataLength){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(data, 4, dataLength-4);
        return data = stream.toByteArray();
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
}
