package org.scu.edu.gossip.services;


import org.scu.edu.gossip.configs.GossipProperty;
import org.scu.edu.gossip.models.ChatMessage;
import org.scu.edu.gossip.models.GossipNode;
import org.scu.edu.gossip.models.GossipNodeStatus;
import org.apache.log4j.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NodeGossiper {

    private static final Logger log = Logger.getLogger(NodeGossiper.class);
            //LoggerFactory.getLogger(NodeGossiper.class);

    private final GossipNode gossipNode;
    private final GossipProperty gossipProperty;
    private final ConcurrentHashMap<String, GossipNode> clusterInfo; //stores all node which are online
    private final MembershipService gossipNodeConnector;
    private final ChatMessageService commChannel;
    private final ConcurrentHashMap<String, Float> uid = new ConcurrentHashMap<>(); //uid stores uniqueID of message they recieved if they recieve same message again, the probabilty of forwarding the message is decreased
    private Boolean stopped = false;
    private List<ChatMessage> chatStorage = new ArrayList<>();

    Thread sender;

   
    public NodeGossiper(InetSocketAddress nodeSocketAddress,
                        GossipProperty gossipProperty) {
        this.gossipProperty = gossipProperty;
        this.gossipNode = new GossipNode(nodeSocketAddress, 0, gossipProperty);
        this.gossipNodeConnector = new MembershipService(nodeSocketAddress.getPort());
        this.commChannel = new ChatMessageService(nodeSocketAddress.getPort());
        this.clusterInfo = new ConcurrentHashMap<>();
        clusterInfo.putIfAbsent(gossipNode.getUniqueId(), gossipNode);

    }

    public NodeGossiper(InetSocketAddress nodeSocketAddress,
                        InetSocketAddress targetSocketAddress,
                        GossipProperty gossipProperty) {
        this(nodeSocketAddress, gossipProperty);
        GossipNode initialTargetNode = new GossipNode(targetSocketAddress, 0, gossipProperty);
        clusterInfo.putIfAbsent(initialTargetNode.getUniqueId(), initialTargetNode);
        this.gossipNode.addKnowNodes(initialTargetNode.getUniqueId(), initialTargetNode);
    }

    /**
     * Start node gossip process
     */
    public void start() {
        startSenderThread();
        startReceiverThread();
        startFailureDetectionThread();
        startChatSender();
        startChatReceiver();
        startReplicationThread();
        //getMemberInfo();
    }

    //Message Sync between nodes
    public void startReplicationThread() {
        (new Thread() {

            @Override
            public void run() {

                {
                    sendDBSyncMessage();
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        log.error(e);
                    }
                }
                
            }


        }).start();
    }
    //Sends pull request when node start, to get all the message he didn't recieve
    public void sendDBSyncMessage() {
        final String msg = "pullInfo";
        String uniqueID = UUID.randomUUID().toString();
        uid.put(uniqueID, 1.00f);
        ChatMessage<String> message = new ChatMessage<>(gossipNode, msg, uniqueID, false);
        gossipChatMessage(message);
    }
    
    //Sends chatStorage to node which requested pullinfo
    public void sendDBInfo(ChatMessage<String> msg) {
        (new Thread() {
            @Override
            public void run() {

                String uniqueID = UUID.randomUUID().toString();
                uid.put(uniqueID, 1.00f);
                ChatMessage<List<ChatMessage>> message = new ChatMessage<>(gossipNode, chatStorage, uniqueID, true);
                commChannel.sendMessage(msg.getSender(), message);
            }
        }).start();
    }

    //Merges chatStorage with recieved chatStorage
    public void mergeDBInfo(ChatMessage<List<ChatMessage>> msg) {
       (new Thread() {
            @Override
            public void run() {
            List<ChatMessage> newStorage = msg.getMessage();
                
                for (ChatMessage data : newStorage) {
                    synchronized(uid){
       
                        if (uid.containsKey(data.getUUID())){
                            log.info("Message already added");
                        } else {
                            
                            uid.putIfAbsent(data.getUUID(), 1f);
                            System.out.println("Recieved Message from "+data.getSender().getPort()+": "+ data.getMessage());
                            log.info("Adding message {"+data.getMessage()+ " From "+ data.getSender().getPort()+"} in chatStorage");
                            chatStorage.add(data);
                        }
                        }
                    }
            }
        }).start();
    }


    //Chat message service
    public void startChatSender() {
        (new Thread() {
            @Override
            public void run() {
                sendChatMessages();
            }
        }).start();
    }

    public void sendChatMessages() {
        try {

            System.out.println("Give input message ");
            try (BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) {
                String input;
                while ((input = stdIn.readLine()) != null) {
                    final String msg = input;
                    String uniqueID = UUID.randomUUID().toString();
                    uid.put(uniqueID, 1.00f);
                    
                    ChatMessage<String> message = new ChatMessage<>(gossipNode, msg, uniqueID, false);
                    if (!chatStorage.contains(message) && !"pullInfo".equals(input)) {
                        synchronized (chatStorage) {
                            log.info("Adding message {"+input+ " From "+gossipNode.getPort()+"} in chatStorage");
                            chatStorage.add(message);
                        }
                    }
                    gossipChatMessage(message);
                }
            }
        } catch (NumberFormatException | IOException e) {
            log.error(e);
        }
    }

    public void gossipChatMessage(ChatMessage<String> message) {
        (new Thread() {
            @Override
            public void run() {
                List<String> targetNodeIds = fetchRandomNodes(gossipProperty.getPeerCount());
                targetNodeIds.forEach((targetNodeId) -> {
                    GossipNode targetNode = clusterInfo.get(targetNodeId);

                    if (clusterInfo.get(targetNodeId) != null) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                commChannel.sendMessage(targetNode, message);
                            }
                        })
                                .start();
                    } else {
                        log.info("Not available port");
                    }
                });

            }
        }).start();
    }

    public void startChatReceiver() {
        (new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        //Accepting Request
                        final Socket s = commChannel.getServerSocket().accept();
                        ChatMessage message = commChannel.receiveMessage(s);
                                String mergingData = "True";
                                //Merging chatStorage if message contains DBinfo
                                if (message.containsDBinfo() ) {
                                    mergeDBInfo(message);
                                } else {
                                    String req = (String) message.getMessage();
                                    if (req.equals("pullInfo")) {
                                        sendDBInfo(message);
                                    } else {
                                        // Create a new thread for each recieve message and applying exponential backoff to control network congestion
                                        runExponentialBackOff(message);
                                    }
                                }
                            
                    } catch (IOException ex) {
                        log.error(ex);
                    }

                }
            }
        }).start();
    }
    
    //stops gossiping same message after its probability decreases by 1/64
    private void runExponentialBackOff(ChatMessage<String> message) {
        (new Thread() {
            @Override
            public synchronized void run() {
                try {
                    if (uid.containsKey(message.getUUID())) {
                        float probability = uid.get(message.getUUID());
                        probability /= 2;
                        if (probability >= (1 / 64f)) {
                            uid.replace(message.getUUID(), probability);
                            Thread.sleep(gossipProperty.getUpdateFrequency().toMillis());
                            log.info("Forwarding Message: "+ message.getMessage() +" Probability changed to "+probability);
                            gossipChatMessage(message);
                        }

                    } else {
                        uid.put(message.getUUID(), 1.00f);
                        chatStorage.add(message);
                        System.out.println("Recieved Message from "+message.getSender().getPort()+": "+message.getMessage());
                        Thread.sleep(gossipProperty.getUpdateFrequency().toMillis());
                        gossipChatMessage(message);
                    }

                } catch (InterruptedException ex) {
                    log.error("Error:"+ex);
                }
            }
        }).start();

    }

    private void startSenderThread() {
        sender = new Thread(() -> {
            while (!stopped) {
                sendGossipMessage();
                try {
                    Thread.sleep(gossipProperty.getUpdateFrequency().toMillis());
                } catch (InterruptedException e) {
                    log.error("Unable to start gossip sender thread", e);
                }
            }
        });
        sender.start();
    }

    //Send gossip to peers to maintain membership list
    private void sendGossipMessage() {
        gossipNode.incrementHeartbeat();
        List<String> targetNodeIds = fetchRandomNodes(gossipProperty.getPeerCount());
        List<GossipNode> clusterNodes = new ArrayList<>(clusterInfo.values());
        for (String targetNodeId : targetNodeIds) {
            GossipNode targetNode = clusterInfo.get(targetNodeId);

            if (clusterInfo.get(targetNodeId) != null) {
                new Thread(() ->
                        gossipNodeConnector.sendGossip(clusterNodes, targetNode.getSocketAddress()))
                        .start();
            } else {
                log.info("Node "+clusterInfo.get(targetNodeId)+" failed and is removed");
            }
        }
    }

    // fetch random nodes from the cluster
    private List<String> fetchRandomNodes(int numberOfPeers) {
        List<String> clusterNodes = new ArrayList<>(clusterInfo.keySet());

        //Remove self from peer list
        clusterNodes.remove(gossipNode.getUniqueId());

        //No Random nodes are picked if cluster size is less than peer count
        if (clusterNodes.size() <= numberOfPeers) {
            return clusterNodes;
        }
        Collections.shuffle(clusterNodes);
        return clusterNodes.subList(0, numberOfPeers);
    }

    private void startReceiverThread() {
        new Thread(() -> {
            while (!stopped) {
                receiveGossipMessage();
            }
        }).start();
    }

    //Receive gossip message
    private void receiveGossipMessage() {
        List<GossipNode> receivedList = gossipNodeConnector.receiveGossip();
        synchronized (clusterInfo) {
            updateMembers(receivedList);
        }
    }

    //Update the Current Members with new Nodes
    public void updateMembers(List<GossipNode> receivedList) {
        for (GossipNode member : receivedList) {
            String id = member.getUniqueId();
            synchronized(clusterInfo){
            if (!clusterInfo.containsKey(id)) {
                clusterInfo.put(id, member);
                //member.setConfig(gossipProperty);
                //member.setLastUpdatedTime();
                if(member.getStatus() == 1 ){
                System.out.println("Node Online: "+member.getPort());
                }
                clusterInfo.putIfAbsent(member.getUniqueId(), member);
                for (Map.Entry<String, GossipNode> n : member.getKnownNodes().entrySet()) {
                    clusterInfo.putIfAbsent(n.getKey(), n.getValue());
                }
            } else {
                GossipNode existingMemberRecord = clusterInfo.get(id);
                existingMemberRecord.update(member);
            }
        }
        }
    }

    private void startFailureDetectionThread() {
        new Thread(() -> {
            while (!stopped) {
                nodeFailureDetector();
                synchronized (clusterInfo) {
                    removeFailedNodes();
                }
                try {
                    Thread.sleep(gossipProperty.getDetectionInterval().toMillis());
                } catch (InterruptedException ie) {
                    log.error("Unable to start failure detection thread", ie);
                }
            }
        }).start();
    }

    ////Detect the failed node
    void nodeFailureDetector() {
        LocalDateTime currentTimestamp = LocalDateTime.now();

        for (String m : clusterInfo.keySet()) {
            GossipNode member = clusterInfo.get(m);
            if (member.getStatus() == 1) {
                LocalDateTime failureDetectionTime = member.timestamp.plus(gossipProperty.getFailureTimeout());
                if (currentTimestamp.isAfter(failureDetectionTime)) {
                    member.setStatus(GossipNodeStatus.NODE_SUSPECT_DEAD);
                    System.out.println("Node Offline: " + clusterInfo.get(m).getPort());
                    log.info("Failed Node detected "+ clusterInfo.get(m));
                }
            }
        }
    }

    //Removing nodes that are failed
    void removeFailedNodes() {
        LocalDateTime currentTimestamp = LocalDateTime.now();
        for (String m : clusterInfo.keySet()) {
            GossipNode member = clusterInfo.get(m);
            if (member.getStatus() == 3) {
                LocalDateTime failureDetectionTime = member.timestamp.plus(gossipProperty.getFailureTimeout());
                if (currentTimestamp.isAfter(failureDetectionTime)) {
                    log.info("Removing the failed node {} " + clusterInfo.get(m));
                    clusterInfo.remove(m); // remove Failed members
                }

            }
        }
    }

    public void stop() {
        stopped = true;
    }

    public void getMemberInfo() {

        new Thread(() ->
        {
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                log.error("Unable to get member info", e);
            }
            synchronized (clusterInfo) {
                log.info("List of hosts");
                clusterInfo.values().forEach(node ->
                        log.info( gossipNode.getUniqueId()+" -> "+node.status));

            }
        }).start();
    }

}