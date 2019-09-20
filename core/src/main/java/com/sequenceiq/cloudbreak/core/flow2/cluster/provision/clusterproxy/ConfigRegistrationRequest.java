package com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

class ConfigRegistrationRequest {
    @JsonProperty
    private String clusterCrn;

    @JsonProperty
    private String uriOfKnox;

    @JsonProperty
    private List<String> aliases;

    @JsonProperty
    private List<ClusterServiceConfig> services;

    @JsonCreator
    ConfigRegistrationRequest(String clusterCrn, List<String> aliases, List<ClusterServiceConfig> services) {
        this.clusterCrn = clusterCrn;
        this.aliases = aliases;
        this.services = services;
    }

    @JsonCreator
    ConfigRegistrationRequest(String clusterCrn, String knoxUrl, List<String> aliases, List<ClusterServiceConfig> services) {
        this(clusterCrn, aliases, services);
        this.uriOfKnox = knoxUrl;
    }

    @Override
    public String toString() {
        return "ConfigRegistrationRequest{" +
                "clusterCrn='" + clusterCrn + '\'' +
                ", aliases=" + aliases +
                ", services=" + services +
                ", uriOfKnox='" + uriOfKnox + '\'' +
                '}';
    }
}
