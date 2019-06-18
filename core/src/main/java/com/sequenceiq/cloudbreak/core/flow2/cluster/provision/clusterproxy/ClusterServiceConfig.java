package com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

class ClusterServiceConfig {
    @JsonProperty
    private String name;

    @JsonProperty
    private List<String> endpoints;

    @JsonProperty
    private List<ClusterServiceCredential> credentials;

    @JsonCreator
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
