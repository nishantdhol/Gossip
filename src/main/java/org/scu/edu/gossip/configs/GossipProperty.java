package org.scu.edu.gossip.configs;

import java.io.Serializable;
import java.time.Duration;

public class GossipProperty implements Serializable {
    private final Duration failureTimeout;
    private final Duration cleanupTimeout;
    private final Duration updateFrequency;
    private final Duration detectionInterval;
    private final int peerCount;

    public GossipProperty(Duration failureTimeout,
                          Duration cleanupTimeout,
                          Duration updateFrequency,
                          Duration detectionInterval,
                          int peerCount) {
        this.failureTimeout = failureTimeout;
        this.cleanupTimeout = cleanupTimeout;
        this.updateFrequency = updateFrequency;
        this.detectionInterval = detectionInterval;
        this.peerCount = peerCount;
    }

    public Duration getFailureTimeout() {
        return failureTimeout;
    }

    public Duration getCleanupTimeout() {
        return cleanupTimeout;
    }

    public Duration getUpdateFrequency() {
        return updateFrequency;
    }

    public Duration getDetectionInterval() {
        return detectionInterval;
    }

    public int getPeerCount() {
        return peerCount;
    }
}
