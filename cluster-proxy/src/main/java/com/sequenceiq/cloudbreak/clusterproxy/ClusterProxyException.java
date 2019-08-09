package com.sequenceiq.cloudbreak.clusterproxy;

public class ClusterProxyException extends RuntimeException {
    public ClusterProxyException(String message) {
        super(message);
    }

    public ClusterProxyException(String message, Throwable cause) {
        super(message, cause);
    }
}
