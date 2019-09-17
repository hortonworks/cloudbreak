package com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

class ConfigRegistrationRequest {
    @JsonProperty
    private String clusterCrn;

    @JsonProperty
    private List<ClusterServiceConfig> services;

    @JsonProperty
    private String uriOfKnox;

    @JsonCreator
    ConfigRegistrationRequest(String clusterCrn, List<ClusterServiceConfig> services) {
        this.clusterCrn = clusterCrn;
        this.services = services;
    }

    @JsonCreator
    ConfigRegistrationRequest(String clusterCrn, String knoxUrl, List<ClusterServiceConfig> services) {
        this(clusterCrn, services);
        this.uriOfKnox = knoxUrl;
    }

    @Override
    public String toString() {
        return "ConfigRegistrationRequest{" +
                "clusterCrn='" + clusterCrn + '\'' +
                ", services=" + services +
                ", uriOfKnox='" + uriOfKnox + '\'' +
                '}';
    }
}
