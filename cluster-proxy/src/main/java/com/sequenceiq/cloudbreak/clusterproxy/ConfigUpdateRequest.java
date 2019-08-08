package com.sequenceiq.cloudbreak.clusterproxy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ConfigUpdateRequest {
    @JsonProperty
    private String clusterCrn;

    @JsonProperty
    private String uriOfKnox;

    @JsonCreator
    public ConfigUpdateRequest(String clusterCrn, String uriOfKnox) {
        this.clusterCrn = clusterCrn;
        this.uriOfKnox = uriOfKnox;
    }

    @Override
    public String toString() {
        return "ConfigUpdateRequest{clusterCrn='" + clusterCrn + '\'' + ", uriOfKnox='" + uriOfKnox + '\'' + '}';
    }
}
