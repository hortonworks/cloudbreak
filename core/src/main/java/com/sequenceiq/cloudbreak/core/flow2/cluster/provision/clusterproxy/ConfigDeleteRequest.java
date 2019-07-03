package com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

class ConfigDeleteRequest {
    @JsonProperty
    private String clusterCrn;

    @JsonCreator
    ConfigDeleteRequest(String clusterCrn) {
        this.clusterCrn = clusterCrn;
    }

    @Override
    public String toString() {
        return "ConfigDeleteRequest{clusterCrn='" + clusterCrn + '\'' + '}';
    }
}
