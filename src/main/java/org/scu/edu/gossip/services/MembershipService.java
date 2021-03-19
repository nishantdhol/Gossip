package org.scu.edu.gossip.services;

import org.apache.logging.log4j.LogManager;
import org.scu.edu.gossip.models.GossipNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;

public class MembershipService {
    private static final Logger log = Logger.getLogger(MembershipService.class);
            //LoggerFactory.getLogger(GossipNodeConnector.class);

    private DatagramSocket datagramSocket;
    private final byte[] receivedBuffer = new byte[8192];
    private final DatagramPacket receivePacket =
            new DatagramPacket(receivedBuffer, receivedBuffer.length);

    public MembershipService(int portToListen) {
        try {
            datagramSocket = new DatagramSocket(portToListen);
        } catch (SocketException e) {
            log.error("Unable to open socket for port "+portToListen, e);
        }
    }

    public List<GossipNode> receiveGossip() {
        List<GossipNode> message = null;
        try {
            datagramSocket.receive(receivePacket);

            try (ObjectInputStream objectInputStream = new ObjectInputStream(
                    new ByteArrayInputStream(receivePacket.getData()))) {

                message = (List<GossipNode>) objectInputStream.readObject();
                log.info("Received a gossip message "+message);

            } catch (ClassNotFoundException e) {
                log.error("Error in receiving message", e);
            }

        } catch (IOException e) {
            log.error("Unable to receive message", e);
        }

        return message;
    }

    public void sendGossip(List<GossipNode> memberList, InetSocketAddress receiver) {
        byte[] bytesToWrite = getBytesToWrite(memberList);
        sendGossipMessage(receiver, bytesToWrite);
    }

    private byte[] getBytesToWrite(List<GossipNode> memberList) {
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        try (ObjectOutput oo = new ObjectOutputStream(bStream)) {
            oo.writeObject(memberList);
        } catch (IOException e) {
            log.error("Unable to write message", e);
        }
        return bStream.toByteArray();
    }

    private void sendGossipMessage(InetSocketAddress target, byte[] data) {
        DatagramPacket packet = null;

        try {
            packet = new DatagramPacket
                    (data, data.length, target.getAddress(), target.getPort());
        } catch (Exception e1) {
            log.error("Data: {"+Arrays.toString(data)+"} length: "+data.length+" target inet: "+target.getAddress()+" target getport: "+target.getPort());

            //log.error("1.data" + Arrays.toString(data) + " length: " + data.length + " target inet" + target.getAddress() + "target getport" + target.getPort());

        }
        try {
            log.info("Sending gossip message to [" + target.toString() + "]");
            datagramSocket.send(packet);
        } catch (IOException e) {

            log.error("Fatal error trying to send: "
                    + packet + " to [" + target.toString() + "]");

        }
    }

}
