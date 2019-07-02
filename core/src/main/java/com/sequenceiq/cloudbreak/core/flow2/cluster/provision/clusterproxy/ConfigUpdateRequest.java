package com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

class ConfigUpdateRequest {
    @JsonProperty
    private String clusterCrn;

    @JsonProperty
    private String uriOfKnox;

    @JsonCreator
    ConfigUpdateRequest(String clusterCrn, String uriOfKnox) {
        this.clusterCrn = clusterCrn;
        this.uriOfKnox = uriOfKnox;
    }

    @Override
    public String toString() {
        return "ConfigUpdateRequest{clusterCrn='" + clusterCrn + '\'' + ", uriOfKnox='" + uriOfKnox + '\'' + '}';
    }
}
