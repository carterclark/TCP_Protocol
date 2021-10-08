
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Objects;

public class Sender {// Client

    // Steps to use:
    // (first time) javac Sender.java
    // java Sender localhost 8080 image.png

    private static String receiverAddress = "";
    private static String inputFile = "";
    private static double dataGrams = 0.0;
    private static int packetSize = 4096; // default buffer will send the data in 4K chunks
    private static int timeOut = 300; // default timeout
    private static int receiverPort = 0;

    private FileInputStream inputStream;
    private int sequence = 0;
    private Packet packet = new Packet();
    private int index = 0;

    // main
    public static void main(String[] args) {
        Sender sender = new Sender();
        sender.run(args);
    }

    // directions for use
    private static void Usage() {
        System.out.println("\n\nMandatory command parameters must be entered in the order displayed here.");
        System.out.println("Parameters in [] are optional and must come before the three mandatory items.");
        System.out.println("-d is the percentage of packets to alter.  -d 2.5 (not implemented)");
        System.out.println("-s is packet size, cannot exceed 4096.  -s 512 (default is 4096)");
        System.out.println("-t is the timeout value.  -t 300 (not implemented)");
        System.out.println("Usage: java Sender [-d #.#] [-s ###] [-t ###] receiver_address receiver_port input_file");
        System.exit(1);
    }

    // parse the command line parameters
    private static void ParseCmdLine(String[] args) {
        int i = 0;
        String arg;

        if (args.length < 3)
            Usage(); // run with no parameters or too few to see usage message

        while (i < args.length) {
            arg = args[i];

            // process any command line switches
            if (arg.startsWith("-")) {

                // optional parameters
                switch (arg.charAt(1)) {
                    case 'd':
                        // if next argument also starts with a - then the value for the command line
                        // switch was not provided
                        if (args[i + 1].startsWith("-")) {
                            System.err.println("-d requires a value");
                            Usage();
                        } else {
                            dataGrams = Double.parseDouble(args[++i]);
                        }
                        break;
                    case 's':
                        if (args[i + 1].startsWith("-")) {
                            System.err.println("-s requires a packet size");
                            Usage();
                        } else {
                            packetSize = Integer.parseInt(args[++i]);
                            if (packetSize > 4096) {
                                System.err.println("Packetsize cannot be greater than 4096");
                                Usage();
                            }
                        }
                        break;
                    case 't':
                        if (args[i + 1].startsWith("-")) {
                            System.err.println("-t requires a timeout value");
                            Usage();
                        } else {
                            timeOut = Integer.parseInt(args[++i]);
                        }
                        break;
                }
                // mandatory parameters
            } else {
                // not a command line switch so must be the filename, receiver address, or
                // receiver port
                // must have at minimum the filename, the receiver address, and the receiver
                // port
                if (i == (args.length - 3)) {
                    receiverAddress = args[i];
                }

                if (i == (args.length - 2)) {
                    receiverPort = Integer.parseInt(args[i]);
                }

                if (i == (args.length - 1)) {
                    inputFile = args[i];
                }
                i++;
            }
        }

        // if values were not provided on commandline the defaults will trigger a usage
        // message
        if (Objects.equals(inputFile, "") || Objects.equals(receiverAddress, "") | receiverPort == 0)
            Usage();
    }

    private void run(String[] args) {
        InetAddress address;
        DatagramSocket senderSocket;

        // verify there are at least three parameters being passed in
        if (args.length < 3) {
            System.out.println("\n\nINSUFFICIENT COMMAND LINE ARGUMENTS\n\n");
            Usage();
        }

        ParseCmdLine(args); // parse the parameters that were passed in

        /*
         * // for testing System.out.println("The packet size will be: " + packetSize);
         * System.out.println("The source file is     : " + inputFile);
         * System.out.println("The timeout is         : " + timeOut);
         * System.out.println("Datagram percentage is : " + dataGrams);
         * System.out.println("Receiver address is    : " + receiverAddress);
         * System.out.println("Receiver port is       : " + receiverPort);
         */

        try {

            inputStream = new FileInputStream(inputFile); // open input stream
            address = InetAddress.getByName(receiverAddress); // convert recieverAddress to an InetAddress
            senderSocket = new DatagramSocket(); // Instantiate the datagram socket
            sequence = 0;
            byte[] buffer = new byte[packetSize]; // create the "send" buffer

            // logging counters/variables
            int packetCount = 0;
            int bytesRead;
            long startOffset = 0;
            long endOffset = 0;

            System.out.println("\nSENDING FILE\n");
            do {
                // read the input file in packetSize chunks, and send them to the server
                bytesRead = inputStream.read(buffer);
                if (bytesRead == -1) {
                    // end of file, tell the receiver that we are done sending
                    buffer = "end".getBytes();
                    DatagramPacket datagramPacket = new DatagramPacket(buffer, 3, address, receiverPort);
                    senderSocket.send(datagramPacket);
                    System.out.println("Sent end packet.  Terminating.");
                    break;
                } else {
                    endOffset += bytesRead;
                    System.out.format("Packet: %4d  -  Start Byte Offset: %8d  -  End Byte Offset: %8d%n",
                            ++packetCount, startOffset, endOffset); // progress logging
                    // start offset will be the offset the prior read ended at which is 0 for the
                    // first read
                    // the position of the last byte read will become the effective 0 for the next
                    // read which will start where the last read stopped
                    startOffset = endOffset;

                    // create and send the packet
                    DatagramPacket packet = new DatagramPacket(buffer, bytesRead, address, receiverPort);
                    senderSocket.send(packet);
                    buffer = new byte[packetSize]; // flush buffer
                }

            } while (true);

            // done, close streams/sockets
            inputStream.close();
            senderSocket.close();

        } catch (FileNotFoundException ex) {
            System.out.println("\n\nUNABLE TO LOCATE OR OPEN THE INPUT FILE: " + inputFile + "\n\n");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
