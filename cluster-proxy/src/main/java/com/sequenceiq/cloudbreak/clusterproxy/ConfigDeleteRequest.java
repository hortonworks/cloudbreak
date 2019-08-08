package com.sequenceiq.cloudbreak.clusterproxy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ConfigDeleteRequest {
    @JsonProperty
    private String clusterCrn;

    @JsonCreator
    public ConfigDeleteRequest(String clusterCrn) {
        this.clusterCrn = clusterCrn;
    }

    @Override
    public String toString() {
        return "ConfigDeleteRequest{clusterCrn='" + clusterCrn + '\'' + '}';
    }
}
