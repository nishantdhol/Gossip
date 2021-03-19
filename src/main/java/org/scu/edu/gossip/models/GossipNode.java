package org.scu.edu.gossip.models;

import static org.scu.edu.gossip.models.GossipNodeStatus.NODE_ACTIVE;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import org.scu.edu.gossip.configs.GossipProperty;

public class GossipNode implements Serializable {
    private final InetSocketAddress address;
    private final String id;
    public LocalDateTime timestamp;
    private long heartbeat;
    public GossipNodeStatus status;
    private GossipProperty config;
 
    private ConcurrentHashMap<String, GossipNode> known_nodes= new ConcurrentHashMap<>();

    public GossipNode(InetSocketAddress address, long initialSequenceNumber, GossipProperty config) {
        this.address = address;
        this.id = address.toString();
        this.heartbeat = initialSequenceNumber;
        this.config = config;
        this.timestamp = LocalDateTime.now();
        this.status = NODE_ACTIVE;
    }

    public String getAddress() {
        return address.getHostName();
    }

    public InetAddress getInetAddress() {
        return address.getAddress();
    }

    public InetSocketAddress getSocketAddress() {
        return address;
    }

    public int getPort() {
        return address.getPort();
    }

    public String getUniqueId() {
        return id;
    }

    public void incrementHeartbeat() {
        ++heartbeat;
        timestamp = LocalDateTime.now();
    }

    public void setStatus(GossipNodeStatus status) {
        this.status = status;
    }

    public int getStatus() {
        return status.statusCode;
    }

    public void update(GossipNode newInfo) {
        if (newInfo.timestamp.isBefore(this.timestamp) ||
                newInfo.timestamp.isEqual(this.timestamp))
            return;

        if (newInfo.heartbeat <= this.heartbeat)
            return;

        this.timestamp = LocalDateTime.now();
        this.heartbeat = newInfo.heartbeat;

        this.setStatus(NODE_ACTIVE);
    }
    
    public ConcurrentHashMap<String, GossipNode> getKnownNodes(){
        return this.known_nodes;
    }

    public void addKnowNodes(String id, GossipNode node){
        known_nodes.putIfAbsent(id, node);
    }
}
