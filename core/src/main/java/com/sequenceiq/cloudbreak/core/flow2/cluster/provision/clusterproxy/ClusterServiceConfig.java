package com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy;

import java.util.List;

class ClusterServiceConfig {
    private String name;

    private List<String> endpoints;

    private List<ClusterServiceCredential> credentials;

    ClusterServiceConfig(String serviceName, List<String> endpoints, List<ClusterServiceCredential> credentials) {
        this.name = serviceName;
        this.endpoints = endpoints;
        this.credentials = credentials;
    }

    @Override
    public String toString() {
        return "ClusterServiceConfig{serviceName='" + name + '\'' + ", endpoints=" + endpoints + ", credentials=" + credentials + '}';
    }
}
