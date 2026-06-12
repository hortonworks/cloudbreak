package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ConfigUpdateRequest {

    @JsonProperty
    private final String clusterCrn;

    @JsonProperty
    private final String uriOfKnox;

    @JsonProperty
    @JsonInclude(Include.NON_NULL)
    private String knoxSecretRef;

    @JsonCreator
    public ConfigUpdateRequest(String clusterCrn, String uriOfKnox) {
        this.clusterCrn = clusterCrn;
        this.uriOfKnox = uriOfKnox;
    }

    @JsonCreator
    public ConfigUpdateRequest(String clusterCrn, String uriOfKnox, String knoxSecretRef) {
        this.clusterCrn = clusterCrn;
        this.uriOfKnox = uriOfKnox;
        this.knoxSecretRef = knoxSecretRef;
    }

    public String getClusterCrn() {
        return clusterCrn;
    }

    public String getUriOfKnox() {
        return uriOfKnox;
    }

    public String getKnoxSecretRef() {
        return knoxSecretRef;
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
                Objects.equals(uriOfKnox, that.uriOfKnox) &&
                Objects.equals(knoxSecretRef, that.knoxSecretRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterCrn, uriOfKnox, knoxSecretRef);
    }

    @Override
    public String toString() {
        return "ConfigUpdateRequest{clusterCrn='" + clusterCrn + '\'' +
                ", uriOfKnox='" + uriOfKnox + '\'' +
                ", knoxSecretRef='" + knoxSecretRef + '\'' + '}';
    }
}
