package com.sequenceiq.cloudbreak.clusterproxy;

import jakarta.ws.rs.WebApplicationException;

public class ClusterProxyWebApplicationException extends WebApplicationException {

    private final ClusterProxyError clusterProxyError;

    public ClusterProxyWebApplicationException(String errorMessage, ClusterProxyError clusterProxyError) {
        super(errorMessage);
        this.clusterProxyError = clusterProxyError;
    }

    public ClusterProxyError getClusterProxyError() {
        return clusterProxyError;
    }
}
