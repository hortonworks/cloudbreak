package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ConfigRegistrationRequest {
    @JsonProperty
    private String clusterCrn;

    @JsonProperty
    private String uriOfKnox;

    @JsonProperty
    private List<String> aliases;

    @JsonProperty
    private List<ClusterServiceConfig> services;

    @JsonCreator
    public ConfigRegistrationRequest(String clusterCrn, List<String> aliases, List<ClusterServiceConfig> services) {
        this.clusterCrn = clusterCrn;
        this.aliases = aliases;
        this.services = services;
    }

    @JsonCreator
    public ConfigRegistrationRequest(String clusterCrn, String knoxUrl, List<String> aliases, List<ClusterServiceConfig> services) {
        this(clusterCrn, aliases, services);
        this.uriOfKnox = knoxUrl;
    }

    public String getClusterCrn() {
        return clusterCrn;
    }

    public String getUriOfKnox() {
        return uriOfKnox;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public List<ClusterServiceConfig> getServices() {
        return services;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConfigRegistrationRequest that = (ConfigRegistrationRequest) o;

        return Objects.equals(clusterCrn, that.clusterCrn) &&
                Objects.equals(uriOfKnox, that.uriOfKnox) &&
                Objects.equals(aliases, that.aliases) &&
                Objects.equals(services, that.services);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterCrn, uriOfKnox, aliases, services);
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
