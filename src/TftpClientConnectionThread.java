
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class TftpClientConnectionThread implements Runnable {
	DatagramSocket sendReceiveSocket;
	int resendAttempts = 3;
	DatagramPacket sendPacket, receivePacket, resendPacket;
	TftpData validData = new TftpData();
	TftpAck validAck = new TftpAck();
	String fileName;
	String filePath = System.getProperty("user.dir") + "/serverFiles/";
	boolean isReadRequest;
	InetAddress destinationAddress;
	int port;

	public TftpClientConnectionThread(boolean isReadRequest, DatagramPacket receivePacket) {

		try {
			sendReceiveSocket = new DatagramSocket();
			sendReceiveSocket.setSoTimeout(2000);
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
			resendPacket = ack.generatePacket(destinationAddress, port);
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
					try {
						this.receiveExpected(blockNumber);
					} catch (Exception e1) {
						outputStream.close();
						System.out.println("Exception: " + e1.getMessage());
						return;
					}
					// Check if it is an error packet
					if (receivePacket.getData()[1] == 5) {
						outputStream.close();
						return;
					}
					// Check if packet is from an unknown transfer ID
					if (receivePacket.getPort() != port) {
						TftpError error = new TftpError(5, "Unknown transfer ID");
						sendReceiveSocket.send(error.generatePacket(receivePacket.getAddress(), receivePacket.getPort()));
						outputStream.close();
						return;
					}
					// Check if not a valid data packet
					if (!validData.validateFormat(receivePacket.getData(), receivePacket.getLength())) {
						TftpError error = new TftpError(4, "Invalid data packet");
						sendReceiveSocket.send(error.generatePacket(receivePacket.getAddress(), port));
						outputStream.close();
						return;
					}
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
					resendPacket = ack.generatePacket(destinationAddress, port);
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
				resendPacket = dataPacket.generatePacket(destinationAddress, port);
				try {
					sendReceiveSocket.send(dataPacket.generatePacket(destinationAddress, port));
				} catch (IOException e1) {
					e1.printStackTrace();
					System.exit(1);
				}

				try {
					this.receiveExpected(blockNumber);
				} catch (Exception e) {
					inputStream.close();
					System.out.println("Exception: " + e.getMessage());
					return;
				}
				// Check if it is an error packet
				if (receivePacket.getData()[1] == 5) {
					inputStream.close();
					return;
				}
				// Check if packet is from an unknown transfer ID
				if (receivePacket.getPort() != port) {
					TftpError error = new TftpError(5, "Unknown transfer ID");
					sendReceiveSocket.send(error.generatePacket(receivePacket.getAddress(), receivePacket.getPort()));
					inputStream.close();
					return;
				}
				// Check if not a valid ack packet
				if (!validAck.validateFormat(receivePacket.getData(), receivePacket.getLength())) {
					TftpError error = new TftpError(4, "Invalid ack packet");
					sendReceiveSocket.send(error.generatePacket(receivePacket.getAddress(), port));
					inputStream.close();
					return;
				} 
				blockNumber++;

			} while (nRead == 512);

			System.out.println("Done sending file: " + fileName + " to client");
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
	
	public void receiveExpected(int blockNumber) throws Exception {
		int timeouts = 0;
		try {
			while (true) {
				try {
					this.receive();
					// Check if it is a data packet
					if (receivePacket.getData()[1] == 3) {
						if (receivePacket.getData()[3] == blockNumber) {
							return;
						} else if (receivePacket.getData()[3] < blockNumber) {
							// Received an old data packets, so we are echoing
							// the ack
							TftpAck ack = new TftpAck(receivePacket.getData()[3]);
							sendPacket = ack.generatePacket(receivePacket.getAddress(), port);
							System.out.println("\nClient: Sending ACK packet in response to duplicate data");
							System.out.println("Block#: " + sendPacket.getData()[2] + sendPacket.getData()[3]);
							sendReceiveSocket.send(sendPacket);

						} else {
							// Received a future block which is invalid
							TftpError error = new TftpError(4, "Invalid block number");
							sendReceiveSocket
									.send(error.generatePacket(receivePacket.getAddress(), receivePacket.getPort()));
						}
					// Check to see if it is an ack packet
					} else if (receivePacket.getData()[1] == 4) {
						if (receivePacket.getData()[3] == blockNumber) {
							return;
						} else if (receivePacket.getData()[3] > blockNumber) {
							// Received a future block which is invalid
							TftpError error = new TftpError(4, "Invalid block number");
							sendReceiveSocket
									.send(error.generatePacket(receivePacket.getAddress(), receivePacket.getPort()));
						}
					} else if (receivePacket.getData()[1] == 5) {
						if (receivePacket.getData()[3] != 5)
							throw new Exception("Stopping data transfer");
					} else if (receivePacket.getData()[1] == 1 || receivePacket.getData()[1] == 2) {
						throw new Exception("Received request packet during data transfer");
					}
				} catch (SocketTimeoutException e) {
					if (timeouts >= resendAttempts) {
						throw new Exception("Connection timed out");
					}
					timeouts++;
					resendLastPacket();
				}
			}
		} catch (IOException e) {
			throw new Exception(e.getMessage());
		}
	}
	
	private void resendLastPacket() throws Exception{
		try {
			sendReceiveSocket.send(resendPacket);
		} catch (IOException e){
			throw new Exception(e.getMessage());
		}
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
