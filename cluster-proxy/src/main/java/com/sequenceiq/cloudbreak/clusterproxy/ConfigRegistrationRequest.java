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

    @JsonProperty
    private List<String> certificates;

    @JsonCreator
    public ConfigRegistrationRequest(String clusterCrn, List<String> aliases, List<ClusterServiceConfig> services, List<String> certificates) {
        this.clusterCrn = clusterCrn;
        this.aliases = aliases;
        this.services = services;
        this.certificates = certificates;
    }

    @JsonCreator
    public ConfigRegistrationRequest(String clusterCrn, String knoxUrl, List<String> aliases, List<ClusterServiceConfig> services, List<String> certificates) {
        this(clusterCrn, aliases, services, certificates);
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
                Objects.equals(services, that.services) &&
                Objects.equals(certificates, that.certificates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterCrn, uriOfKnox, aliases, services, certificates);
    }

    @Override
    public String toString() {
        return "ConfigRegistrationRequest{" +
                "clusterCrn='" + clusterCrn + '\'' +
                ", aliases=" + aliases +
                ", services=" + services +
                ", uriOfKnox='" + uriOfKnox + '\'' +
                ", certificates=" + certificates +
                '}';
    }
}
