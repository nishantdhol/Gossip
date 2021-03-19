package org.scu.edu.gossip.models;


import java.io.Serializable;

public class ChatMessage<T> implements Serializable
{
    private T message;
    private final String uuid;
    private boolean hasDBInfo;
    private GossipNode sender;
   
    public ChatMessage(GossipNode sender, T message, String id, boolean hasDBInfo)
    {
        this.message = message;
        this.uuid = id;
        this.hasDBInfo = hasDBInfo;
        this.sender = sender;
    }

    public String getUUID()
    {
        return uuid;
    }

    public boolean containsDBinfo()
    {
        return this.hasDBInfo;
    }

    public T getMessage()
    {
        return message;
    }

    public GossipNode getSender()
    {
        return sender;
    }


}
