package org.scu.edu.gossip.models;

public enum GossipNodeStatus {
    NODE_ACTIVE(1),
    NODE_DEAD(2),
    NODE_SUSPECT_DEAD(3);

    int statusCode;

    GossipNodeStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    int getStatusCode() {
        return statusCode;
    }
}
