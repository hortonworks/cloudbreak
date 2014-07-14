package com.sequenceiq.periscope.service;

public class ClusterNotFoundException extends RuntimeException {

    private final String clusterId;

    public ClusterNotFoundException(String clusterId) {
        super("Cluster not found: " + clusterId);
        this.clusterId = clusterId;
    }

    public String getClusterId() {
        return clusterId;
    }
}
