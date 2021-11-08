import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import objects.Packet;

import static util.Utility.GOOD_CHECKSUM;
import static util.Utility.SENT;
import static util.Utility.convertPacketToByteArray;

public class Sender extends SenderBase {// Client

    // Steps to use:
    // javac Sender.java
    // java Sender localhost 8080 image.png

    // main
    public static void main(String[] args) {
        Sender sender = new Sender();
        sender.run(args);
    }

    private void run(String[] args) {

        // verify there are at least three parameters being passed in
        if (args.length < 3) {
            System.out.println("\n\nINSUFFICIENT COMMAND LINE ARGUMENTS\n\n");
            Usage();
        }
        ParseCmdLine(args); // parse the parameters that were passed in
        try {
            inputStream = new FileInputStream(inputFile); // open input stream

            file = new File(inputFile);
            dataSize = (int) file.length() / numOfFrames++;

            address = InetAddress.getByName(receiverAddress); // convert receiverAddress to an InetAddress
            serverSocket = new DatagramSocket(); // Instantiate the datagram socket
            dataToSend = new byte[dataSize]; // create the "send" buffer
            byte[] dataToReceive = new byte[dataSize]; // create the "receive" buffer

            // logging counters/variables
            packetCount = 1;
            startOffset = 0;
            long endOffset = 0;

            System.out.println("\nSENDING FILE\n");
            do {
                startTime = System.currentTimeMillis();
                // read the input file in packetSize chunks, and send them to the server
                bytesRead = inputStream.read(dataToSend);
                if (bytesRead == -1) {
                    // end of file, tell the receiver that we are done sending
                    dataToSend = "end".getBytes();
                    DatagramPacket datagramPacket =
                        new DatagramPacket(dataToSend, dataToSend.length, address, receiverPort);
                    serverSocket.send(datagramPacket);
                    System.out.println("Sent end packet.  Terminating.");
                    break;
                } else {
                    endOffset += bytesRead;
                    printSenderInfo(endOffset, SENT);
                    startOffset = endOffset;

                    // sending as packet object
                    byte[] packetDataToSend = convertPacketToByteArray(
                        new Packet(GOOD_CHECKSUM, bytesRead, startOffset, packetCount, dataToSend));
                    datagramPacketToSend =
                        new DatagramPacket(packetDataToSend, packetDataToSend.length, address, receiverPort);
                    serverSocket.send(datagramPacketToSend);

                    //get acknowledgements from receiver
                    validatePacketFromReceiver(serverSocket, dataToReceive, startOffset, bytesRead, packetCount);

                    packetCount++;
                    dataToSend = new byte[dataSize]; // flush buffer
                    datagramPacketToSend = null; // flush packet

                }
            } while (true);
            // done, close streams/sockets
            inputStream.close();
            serverSocket.close();
        } catch (FileNotFoundException ex) {
            System.out.println("\n\nUNABLE TO LOCATE OR OPEN THE INPUT FILE: " + inputFile + "\n\n");
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }
}
