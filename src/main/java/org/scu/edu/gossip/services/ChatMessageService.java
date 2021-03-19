package org.scu.edu.gossip.services;

import org.scu.edu.gossip.models.ChatMessage;
import org.scu.edu.gossip.models.GossipNode;
import org.apache.log4j.Logger;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;

public class ChatMessageService {
    private static final Logger log = Logger.getLogger(ChatMessageService.class);
    private ServerSocket datagramSocket;

    public ChatMessageService(int portToListen) {
        try {
            datagramSocket = new ServerSocket(portToListen);
        }  catch (IOException e) {
            log.error("Unable to open socket", e);
        }
    }
    public ServerSocket getServerSocket()
    {
        return datagramSocket;
    }


    public void sendMessage(GossipNode node, ChatMessage message) {
        try {
                Socket s = new Socket(node.getInetAddress(), node.getPort());
                ObjectOutputStream data = new ObjectOutputStream(s.getOutputStream());
                data.writeObject(message);
                data.flush();
                s.close();
                log.info("Sent " +message.getMessage() +" to process "+ node.getSocketAddress()+" system time is "+LocalDateTime.now());
        } catch (Exception ex) {
            log.error("Unable to send message", ex);
        }

    }

    public ChatMessage receiveMessage(Socket s) {
        try
        {
            ObjectInputStream in = new ObjectInputStream(s.getInputStream());
            ChatMessage message = (ChatMessage)in.readObject();
            log.info("Recieved "+message.getMessage()+" from " +s.getPort()+ " system time is "+LocalDateTime.now());
            return message;

        }
        catch (IOException | ClassNotFoundException e)
        {
        }
        return null;
    }
}