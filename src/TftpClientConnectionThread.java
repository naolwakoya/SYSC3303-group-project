
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class TftpClientConnectionThread implements Runnable {
	DatagramSocket sendReceiveSocket;
	DatagramPacket sendPacket, receivePacket;
	String fileName;
	String filePath = System.getProperty("user.dir") + "/serverFiles/";
	boolean isReadRequest;
	InetAddress destinationAddress;
	int port;

	public TftpClientConnectionThread(boolean isReadRequest, DatagramPacket receivePacket) {

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
	public void run() {
		System.out.println("its in the RUN");
		if (isReadRequest) {
			fileName = extractFileName(receivePacket.getData(), receivePacket.getData().length);
			sendFile();
		} else {
			fileName = extractFileName(receivePacket.getData(), receivePacket.getData().length);
			receiveFile();
		}
		// Close the sockets once complete
		sendReceiveSocket.close();
	}

	public void receiveFile() {
		try {
			// Send acknowledgement packet
			TftpAck ack = new TftpAck(0);
			try {
				sendReceiveSocket.send(ack.generatePacket(destinationAddress, port));
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			File file = new File(filePath + fileName);
			if (file.exists()) {
				TftpError error = new TftpError(6, fileName + " already exists");
				sendReceiveSocket.send(error.generatePacket(destinationAddress, port));
				return;
			}
			if (!file.isAbsolute()) {
				TftpError error = new TftpError(2, "Trying to access file in restricted area");
				sendReceiveSocket.send(error.generatePacket(destinationAddress, port));
				return;
			}
			if (!file.getParentFile().canWrite()) {
				TftpError error = new TftpError(2, "Cannot write to a read-only folder");
				sendReceiveSocket.send(error.generatePacket(destinationAddress, port));
				return;
			}
			byte[] fileData;
			FileOutputStream outputStream = new FileOutputStream(file);
			int blockNumber = 1;
			do {
				try {
					this.receive();

					if (file.canWrite()) {
						fileData = extractFromDataPacket(receivePacket.getData(), receivePacket.getLength());
						outputStream.write(fileData);
						outputStream.getFD().sync();
					} else {
						TftpError error = new TftpError(2, "Cannot write to a read-only folder");
						sendReceiveSocket.send(error.generatePacket(destinationAddress, port));
						return;
					}
					// Send acknowledgement packet
					ack = new TftpAck(blockNumber);
					try {
						sendReceiveSocket.send(ack.generatePacket(destinationAddress, port));
						blockNumber++;
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}

				} catch (SyncFailedException e) {
					outputStream.close();
					file.delete();
					TftpError error = new TftpError(3, "Disk full or allocation exceeded");
					sendReceiveSocket.send(error.generatePacket(destinationAddress, port));
					return;
				}
			} while (!(fileData.length < 512));
			System.out.println("Done receiving file: " + fileName + " from client");
			outputStream.close();
			
		} catch (FileNotFoundException e1) {
			new File(filePath + fileName).delete();
			TftpError error = new TftpError(1, "Could not find: " + fileName);
			try {
				sendReceiveSocket.send(error.generatePacket(destinationAddress, port));
				return;
			} catch (IOException e11) {
				e11.printStackTrace();
			}
		} catch (IOException e) {
			new File(filePath + fileName).delete();
			TftpError error = new TftpError(3, "Disk full or allocation exceeded");
			try {
				sendReceiveSocket.send(error.generatePacket(destinationAddress, port));
				return;
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/*
	 * Sends the file to the client via tftp data packets
	 */
	public void sendFile() {

		int blockNumber = 1;
		FileInputStream inputStream;

		try {
			String filePath = System.getProperty("user.dir") + "/serverFiles/" + fileName;

			File file = new File(filePath);
			// Check if file exists (error code 1)
			if (!file.exists()) {
				throw new FileNotFoundException();
			}
			// Check if area can be accessed (error code 2)
			if (!file.isAbsolute()) {
				TftpError error = new TftpError(2, "Cant access file in folder");
				sendReceiveSocket.send(error.generatePacket(destinationAddress, port));
				return;
			}

			int nRead = 0;
			inputStream = new FileInputStream(file);
			// Read files of max 512 byte chunks
			byte[] data = new byte[512];
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
			try {
				TftpError error = new TftpError(1, "Could not find:" + fileName);
				sendReceiveSocket.send(error.generatePacket(destinationAddress, port));
				return;
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (IOException e) {
			System.out.println("IOException: " + e.getMessage());
		}
	}

	/*
	 * Waits to receive a packet from the sendReceive socket
	 */
	public void receive() {
		// Create a DatagramPacket for receiving packets
		byte receive[] = new byte[1024];
		receivePacket = new DatagramPacket(receive, receive.length);

		try {
			// Block until a datagram is received via sendReceiveSocket.
			sendReceiveSocket.receive(receivePacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		// Process the received datagram
		System.out.println("Received packet:");
		System.out.println("From host: " + receivePacket.getAddress());
		System.out.println("Host port: " + receivePacket.getPort());
		System.out.println("Packet length: " + receivePacket.getLength());
		System.out.println("Containing: " + receivePacket.getData().toString());
		String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
		System.out.println("String form: " + received + "\n");
	}

	/*
	 * returns the byte array of the data in the tftp data packet
	 */
	public byte[] extractFromDataPacket(byte[] data, int dataLength) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(data, 4, dataLength - 4);
		return data = stream.toByteArray();
	}

	/*
	 * returns the filename from the request packet
	 */
	public String extractFileName(byte[] data, int dataLength) {
		int i = 1;
		StringBuilder sb = new StringBuilder();
		while (data[++i] != 0 && i < dataLength) {
			sb.append((char) data[i]);
		}
		return sb.toString();
	}
}
