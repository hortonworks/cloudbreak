package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.Optional;

public class ClusterProxyException extends RuntimeException {

    private final Optional<ClusterProxyError> clusterProxyError;

    public ClusterProxyException(String message, Optional<ClusterProxyError> clusterProxyError) {
        super(message);
        this.clusterProxyError = clusterProxyError;
    }

    public ClusterProxyException(String message, Throwable cause) {
        super(message, cause);
        clusterProxyError = Optional.empty();
    }

    public Optional<ClusterProxyError> getClusterProxyError() {
        return clusterProxyError;
    }
}
