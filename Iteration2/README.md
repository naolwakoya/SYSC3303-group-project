
`Iteration #2 Team 4`
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


#Project Specification
This project design and implement a file transfer system based on the TFTP protocol
specification which is available on the course website.
ìREADME.txt file explaining the names of your files, set up instructions, etc.
Breakdown of responsibilities of each team member for this iteration
UML class diagram
UCMs for a read file transfer and a write file transfer, including the error simulator
Detailed set up and test instructions, including test files used
Code (.java files, all required Eclipse files, etc.)

#Responsibilities
**responsibilities
  -> Naol - Client.java, README file and timing diagrams
    -> Melaku - TftpServer.java, TftpServerControl.java and TftpClientConnectionThread.java
      -> Raymond Wu - Packet class, ErrorSimulator
        -> Chengyang Liu - UML diagram and TftpError.java
          -> Keith Ko - ErrorSimulator


##Instruction to Run
1. Start Server
  2. Start ErrorSimulator (not necessary if in normal mode but won't affect program)
    3. Start Client
      To test the client in test mode you must supply the command line argument serverFiles
      If you need help for the command type "help" on the Console


#Issue
-Right now ErrorSimulator can not exit, there is a thread written up that
	does this similar to our serverShutdown thread.  However this method requires checking
	constantly for the users input for 'quit' but when error sim checks what error the
	user would like to do then its taking the input for the shutdown thread over the asking
	error.  We couldn't think of a way to fix this, any suggestions?
