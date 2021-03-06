package error;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import objects.Packet;

import static util.Constants.BAD_CHECKSUM;
import static util.Constants.ERR;
import static util.Constants.SENDING;
import static util.Constants.SENT;
import static util.Utility.convertPacketToDatagram;
import static util.Utility.getAckStatus;

public class ReceiverErrorHandler {

    public static String sendBadChecksumToSender(DatagramSocket serverSocket, DatagramPacket receivedDatagram)
        throws IOException {
        Packet packetToSender = new Packet();
        packetToSender.setCheckSum(BAD_CHECKSUM);

        DatagramPacket datagramPacket =
            convertPacketToDatagram(packetToSender, receivedDatagram.getAddress(), receivedDatagram.getPort());
        // sending an error to the sender validator
        serverSocket.send(datagramPacket);

        // receiving corrupted ack from sender
        serverSocket.receive(receivedDatagram);

        return getAckStatus(new String(receivedDatagram.getData()));

    }
}
