package com.sequenceiq.freeipa.service.polling.clusterproxy;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyRegistrationClient;

public class ServiceEndpointHealthPollerObject {

    private final String clusterIdentifier;

    private final ClusterProxyRegistrationClient client;

    public ServiceEndpointHealthPollerObject(String clusterIdentifier, ClusterProxyRegistrationClient client) {
        this.clusterIdentifier = clusterIdentifier;
        this.client = client;
    }

    public String getClusterIdentifier() {
        return clusterIdentifier;
    }

    public ClusterProxyRegistrationClient getClient() {
        return client;
    }
}
