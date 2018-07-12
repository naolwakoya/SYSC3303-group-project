
`Iteration #1`
***Carleton University
Department of Systems and Computer Engineering
SYSC3303 Real Time Concurrent System - Summer 2018****
**Naol Gushu _____ 100911600
**Melaku Semaw ____101059910
**Keith Ko_________100973372
**Chengyang Liu____101011773
**Raymond Wu_______100938326  

Files:
Client.java - the Client program that is able to perform read or write requests to a server
ErrorSimulator.java - The error simulator that currently only passes the packets from client-server
Server.java - The Server program that receives requests from the client and executes the requests
TftpAck.java - An object representing the data for a TFTP ack packet
TftpData.java - An object representing the data for a TFTP data packet
TftpPacket.java - An abstract class for generating datagram packets using the different types of packets
TftpRequest.java - An object representing the data for a TFTP RRQ an WRQ packet

Set up Instructions:
First run the server as a java application.
Then, run the client as a java application.
Make sure the clientFiles and serverFiles folders are present in the project
Enter the command read (filename) or write (filename) in the client command window to execute the request.
Once complete you can type stop in the client command window to exit the program.


