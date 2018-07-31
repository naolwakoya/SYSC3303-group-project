
`Iteration #3 Team 4`
***Carleton University
Department of Systems and Computer Engineering
SYSC3303 Real Time Concurrent System - Summer 2018****
Naol Gushu _____ 100911600
Melaku Semaw ____101059910
Keith Ko_________100973372
Chengyang Liu____101011773
Raymond Wu_______100938326

#File included
src -> Client.java, ErrorSimulator.java, TftpAck.java, TftpClientConnectionThread.java,
TftpData.java, TftpError.java, TftpPacket.java, TftpRequest.java,
TftpServer.java and TftpServerControl.java
  serverFiles -> test.txt
  clientFile -> something.txt
  Timing&UML Diagrams -> UML diagram and Timing datagrams
  README.md


#Project Specification
This project design and implement a file transfer system based on the TFTP protocol
specification which is available on the course website.
For this part, assume that I/O errors can occur, so TFTP ERROR packets dealing with
this (Error Code 1, 2, 3, 6) must be prepared, transmitted, received, or handled.
UML class diagram UCMs for a read file transfer and a write file transfer, including the error simulator Detailed set up and test instructions, including test files used Code (.java files, all required Eclipse files, etc.)

#Testing
- To test the file type " read Anything.txt" it should give us file not found which is Error Code: 1, If you make a txt file in the serverFiles folder and make it hidden it give us it will give us
Access violation is like if the file is not readable or writeable which is Error Code 2 Access violation, Error code 6: to test that type "read something.txt" it should give us file already exisist, can't override.


#Responsibilities
**responsibilities
  -> Naol - Client.java, README file and UML and Timing Diagram
    -> Melaku - TftpServer.java, TftpServerControl.java and TftpClientConnectionThread.java
      -> Raymond Wu - Packet class, ErrorSimulator.java, Client.java
        -> Chengyang Liu - UML diagram and TftpError.java
          -> Keith Ko - ErrorSimulator.java


##Instruction to Run
1. Start Server
  2. Start ErrorSimulator
    3. Start Client
      To test the client in test mode you must supply the command line argument serverFiles and type "read test.txt" to read a file and to write a file type
      "write something.txt" and you will be able to type a text and press enter
      If you need help for the command type "help" on the Console
#issue
ErrorStimulator doesn't work properly for now
