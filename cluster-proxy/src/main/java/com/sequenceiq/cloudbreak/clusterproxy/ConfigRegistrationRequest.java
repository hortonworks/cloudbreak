package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class ConfigRegistrationRequest {
    @SuppressFBWarnings("URF_UNREAD_FIELD")
    @JsonProperty
    private String accountId;

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

    @JsonProperty
    private boolean useTunnel;

    @JsonProperty
    private List<TunnelEntry> tunnels;

    @JsonCreator
    public ConfigRegistrationRequest(String clusterCrn, String knoxUrl, String accountId, boolean useTunnel, List<TunnelEntry> tunnelEntries,
            List<String> aliases, List<ClusterServiceConfig> services, List<String> certificates) {
        this.clusterCrn = clusterCrn;
        this.uriOfKnox = knoxUrl;
        this.accountId = accountId;
        this.useTunnel = useTunnel;
        this.tunnels = tunnelEntries;
        this.aliases = aliases;
        this.services = services;
        this.certificates = certificates;
    }

    public String getClusterCrn() {
        return clusterCrn;
    }

    public List<ClusterServiceConfig> getServices() {
        return services;
    }

    //CHECKSTYLE:OFF: CyclomaticComplexity
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConfigRegistrationRequest that = (ConfigRegistrationRequest) o;
        return useTunnel == that.useTunnel &&
                Objects.equals(accountId, that.accountId) &&
                Objects.equals(clusterCrn, that.clusterCrn) &&
                Objects.equals(uriOfKnox, that.uriOfKnox) &&
                Objects.equals(aliases, that.aliases) &&
                Objects.equals(services, that.services) &&
                Objects.equals(certificates, that.certificates) &&
                Objects.equals(tunnels, that.tunnels);
    }
    //CHECKSTYLE:ON

    @Override
    public int hashCode() {
        return Objects.hash(accountId, clusterCrn, uriOfKnox, aliases, services, certificates, useTunnel, tunnels);
    }

    @Override
    public String toString() {
        return "ConfigRegistrationRequest{" +
                "accountId='" + accountId + '\'' +
                ", clusterCrn='" + clusterCrn + '\'' +
                ", uriOfKnox='" + uriOfKnox + '\'' +
                ", aliases=" + aliases +
                ", services=" + services +
                ", certificates=" + certificates +
                ", useTunnel=" + useTunnel +
                ", tunnels=" + tunnels +
                '}';
    }
}
