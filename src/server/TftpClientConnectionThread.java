package server;
import Iter02.TftpAck;
import Iter02.TftpData;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class TftpClientConnectionThread implements Runnable {
    DatagramSocket sendReceiveSocket, receiveSocket;
    DatagramPacket sendPacket, receivePacket;
    String fileName;
    private InetAddress address;
    private int port;

    public TftpClientConnectionThread(DatagramPacket packet){

        this.receivePacket = packet;
        try {
            //Create a datagram socket for sending packets
            sendReceiveSocket = new DatagramSocket();

        }
        catch(SocketException se){
            se.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void run (){
        System.out.println("its in the RUN");
        while(!sendReceiveSocket.isClosed()){



            port = receivePacket.getPort();
            address = receivePacket.getAddress();

            byte data[]=receivePacket.getData();

            // Check if it is a write request

            if (data[1]==2)
            {
                System.out.println("its in the Write");
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
                System.out.println("its in the Read");
                fileName = extractFileName(data,data.length);

                // Send empty data packet with block number 1
                TftpData dat = new TftpData(1,null,0);
                try {
                    sendReceiveSocket.send(dat.generatePacket(receivePacket.getAddress(), receivePacket.getPort()));
                }catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                sendFile();
            }

        }
        //Close the sockets once complete
        receiveSocket.close();
        sendReceiveSocket.close();
    }


    public void receiveFile(){
        try {
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
                    sendReceiveSocket.send(dataPacket.generatePacket(InetAddress.getLocalHost(), receivePacket.getPort()));
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
