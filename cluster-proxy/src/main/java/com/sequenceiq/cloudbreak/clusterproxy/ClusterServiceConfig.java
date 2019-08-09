package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClusterServiceConfig {
    @JsonProperty
    private String name;

    @JsonProperty
    private List<String> endpoints;

    @JsonProperty
    private List<ClusterServiceCredential> credentials;

    @JsonCreator
    public ClusterServiceConfig(String serviceName, List<String> endpoints, List<ClusterServiceCredential> credentials) {
        this.name = serviceName;
        this.endpoints = endpoints;
        this.credentials = credentials;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ClusterServiceConfig that = (ClusterServiceConfig) o;

        return Objects.equals(name, that.name) &&
                Objects.equals(endpoints, that.endpoints) &&
                Objects.equals(credentials, that.credentials);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, endpoints, credentials);
    }

    @Override
    public String toString() {
        return "ClusterServiceConfig{serviceName='" + name + '\'' + ", endpoints=" + endpoints + ", credentials=" + credentials + '}';
    }
}
