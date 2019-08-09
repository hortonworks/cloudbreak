package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.Objects;

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

    public String getClusterCrn() {
        return clusterCrn;
    }

    public String getUriOfKnox() {
        return uriOfKnox;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConfigUpdateRequest that = (ConfigUpdateRequest) o;

        return Objects.equals(clusterCrn, that.clusterCrn) &&
                Objects.equals(uriOfKnox, that.uriOfKnox);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterCrn, uriOfKnox);
    }

    @Override
    public String toString() {
        return "ConfigUpdateRequest{clusterCrn='" + clusterCrn + '\'' + ", uriOfKnox='" + uriOfKnox + '\'' + '}';
    }
}
