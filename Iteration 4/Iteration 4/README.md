`Iteration #2 Team 4`
***Carleton University
Department of Systems and Computer Engineering
SYSC3303 Real Time Concurrent System - Summer 2018****
Naol Gushu _____ 100911600
Melaku Semaw ____101059910
Keith Ko_________100973372
Chengyang Liu____101011773
Raymond Wu_______100938326

## Responsibilities
-> Naol - Client.java, README file and fixing bugs
  -> Melaku - TftpServer.java, fixing bugs and TftpClientConnectionThread.java
    -> Raymond Wu - Packet class, ErrorSimulator, loss, delay and verification testing
      -> Chengyang Liu - diagrams and fixing bugs
        -> Keith Ko - ErrorSimulator and verification testing



# Files
src ->
  - Client.java --------------Code for client that will send Read or Write request
  - TftpServer.java-----------Code for server user interface, launches controlThread
  -  ErrorSimulator.java------Code for forward messages back and forth between client
                              and server.
  -  TftpAck.java-------------Code for acknowledgement data
  -  TftpClientConnectionThread.java----Code for server to check weather it's a valid packet
                                        or not it receive or read requests from the server

  - TftpData.java
  - TftpError.java
  - TftpPacket.java
  - TftpRequest.java,
  - TftpServerControl.java
serverFiles
  - test.txt
clientFile  
  - something.txt
Diagrams

##Project Specification
 -> To handle network errors. Packets can be lost, delayed, and duplicated.
 TFTP's “wait for acknowledgment/timeout/retransmit” protocol helps deal with this. Our program
contain the fix for the Sorcerer's Apprentice bug (i.e. duplicate ACKs must not be acknowledged, and only
the side that is currently sending DATA packets is required to retransmit after a timeout, though both sides may
retransmit).

##Setup instruction
- If you do not have eclipse, download it from here: https://eclipse.org/downloads/
- And install it. Make sure you downloaded the java version of eclipse! then, Open eclipse
- Click file, at the top left of the screen
- Click the 'import' button on the menu that appears
- Select 'Existing projects into workspace', Click next
- Click the browse button next to the 'Select root directory' prompt
- Navigate to the folder where the project was saved in the first step
- Click the root folder of the project, Then click 'ok'
- Click the checkbox next to the name of our project to select it
- Click 'finish'. The project is now imported.

## Instructions to run
1. Start Server
  2. Start ErrorSimulator (not necessary if in normal mode but won't affect program)
    3. Start Client
      - To test the client in test mode you must supply the command line argument serverFiles
      If you need help for the command type "help" on the Console
      then type read something.txt on the Client Console then choose from the options to send the
      packet for instance for normal mode press 0 and enter.
      - For error simulator mode: enter `test`

**write:
  1) Enter `write` on client Console
  2) Enter a file name found in the serverFile mentioned above
    i.e 'write something.txt'

**Read:
  1) Enter read on the client Console
  2) Enter a file name found in the clientFile mentioned above
    i.e 'read test.txt'

##Instruction for Test cases
Test Case: Delayed Packet
  1) Start server and Client in verbose and test modes
  2) Start Error simulator. Setup desired lost packet error scenario using error simulator interface
    2.1) Chose Error category 1
    2.2) choose Desired packet type to delay (read, write, data, ACK, error)
    2.3) chose a time in ms to delay packet by
      - Socket Timeout is 2000 ms.
      - If delayed less than 2000 ms, transfer is not affected except for small delay
      - If delayed more than 2000 ms, client/server will resend packets in place to continue the
        transfer.
      - If delayed too large, the delayed packet will be receive after the end of the transfer.
      Try to keep delays than 1050 ms, (i.e. 2010 - 2050 ms), to ensure the packet is transmitted during
      the transfer.

    3) start the required read or write operation to trigger the lost packet.

Test Case: Lost Packet
  1) Start server and Client in verbose and test modes
  2) Start ErrorSimulator. Setup desire lost packet error scenario using error simulator interface
    2.1) Chose Error category 2
    2.2) Chose Desired packet type to delay (read, write, data, ACK, error)
    2.3) If DATA or ACK, chose packet number to trigger the fault.
Test Case: Duplicate Packet
  1) Start server and Client in verbose and test modes
  2) Start Error simulator. Setup desired lost packet error scenario using error simulator interface
    2.1) Chose Error category 3
    2.2) choose Desired packet type to delay (read, write, data, ACK, error)
    2.3) chose a time in ms to delay packet by
  3) start the required read or write operation to trigger the lost packet.
  4) An identical copy file should be found
  5) In the logs of each of the 3 programs, the system behaviour for a duplicate packet is shown
