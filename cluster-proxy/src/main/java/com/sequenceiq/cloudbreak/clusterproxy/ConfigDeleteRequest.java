package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ConfigDeleteRequest {

    @JsonProperty
    private final String clusterCrn;

    @JsonCreator
    public ConfigDeleteRequest(String clusterCrn) {
        this.clusterCrn = clusterCrn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConfigDeleteRequest that = (ConfigDeleteRequest) o;

        return Objects.equals(clusterCrn, that.clusterCrn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterCrn);
    }

    @Override
    public String toString() {
        return "ConfigDeleteRequest{clusterCrn='" + clusterCrn + '\'' + '}';
    }
}
