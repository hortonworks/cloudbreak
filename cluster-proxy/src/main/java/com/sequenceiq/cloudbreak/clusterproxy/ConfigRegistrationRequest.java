package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class ConfigRegistrationRequest {

    @SuppressFBWarnings("URF_UNREAD_FIELD")
    @JsonProperty
    private final String accountId;

    @JsonProperty
    private final String clusterCrn;

    @JsonProperty
    private final String environmentCrn;

    @JsonProperty
    private final String uriOfKnox;

    @JsonProperty
    private final List<String> aliases;

    @JsonProperty
    private final List<ClusterServiceConfig> services;

    @JsonProperty
    private final List<String> certificates;

    @JsonProperty
    private final boolean useTunnel;

    @JsonProperty
    private final List<TunnelEntry> tunnels;

    @JsonProperty("useCCMv2")
    private final boolean useCcmV2;

    @JsonProperty
    private final List<CcmV2Config> ccmV2Configs;

    @JsonProperty
    private final boolean tlsStrictCheck;

    @JsonCreator
    public ConfigRegistrationRequest(String clusterCrn, String environmentCrn, String knoxUrl, String accountId, boolean useTunnel,
            List<TunnelEntry> tunnelEntries, List<String> aliases, List<ClusterServiceConfig> services, List<String> certificates, boolean useCcmV2,
            List<CcmV2Config> ccmV2Configs, boolean tlsStrictCheck) {
        this.clusterCrn = clusterCrn;
        this.environmentCrn = environmentCrn;
        this.uriOfKnox = knoxUrl;
        this.accountId = accountId;
        this.useTunnel = useTunnel;
        this.tunnels = tunnelEntries;
        this.aliases = aliases;
        this.services = services;
        this.certificates = certificates;
        this.useCcmV2 = useCcmV2;
        this.ccmV2Configs = ccmV2Configs;
        this.tlsStrictCheck = tlsStrictCheck;
    }

    public String getClusterCrn() {
        return clusterCrn;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public List<ClusterServiceConfig> getServices() {
        return services;
    }

    public List<CcmV2Config> getCcmV2Configs() {
        return ccmV2Configs;
    }

    public boolean isUseCcmV2() {
        return useCcmV2;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getUriOfKnox() {
        return uriOfKnox;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public List<String> getCertificates() {
        return certificates;
    }

    public boolean isUseTunnel() {
        return useTunnel;
    }

    public List<TunnelEntry> getTunnels() {
        return tunnels;
    }

    public boolean isTlsStrictCheck() {
        return tlsStrictCheck;
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
                useCcmV2 == that.useCcmV2 &&
                tlsStrictCheck == that.tlsStrictCheck &&
                Objects.equals(accountId, that.accountId) &&
                Objects.equals(clusterCrn, that.clusterCrn) &&
                Objects.equals(environmentCrn, that.environmentCrn) &&
                Objects.equals(uriOfKnox, that.uriOfKnox) &&
                Objects.equals(aliases, that.aliases) &&
                Objects.equals(services, that.services) &&
                Objects.equals(certificates, that.certificates) &&
                Objects.equals(tunnels, that.tunnels) &&
                Objects.equals(ccmV2Configs, that.ccmV2Configs);
    }
    //CHECKSTYLE:ON

    @Override
    public int hashCode() {
        return Objects.hash(accountId, clusterCrn, environmentCrn, uriOfKnox, aliases,
                services, certificates, useTunnel, tunnels, useCcmV2, ccmV2Configs, tlsStrictCheck);
    }

    @Override
    public String toString() {
        return "ConfigRegistrationRequest{" +
                "accountId='" + accountId + '\'' +
                ", clusterCrn='" + clusterCrn + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", uriOfKnox='" + uriOfKnox + '\'' +
                ", aliases=" + aliases +
                ", services=" + services +
                ", certificates=" + certificates +
                ", useTunnel=" + useTunnel +
                ", tunnels=" + tunnels +
                ", useCcmV2=" + useCcmV2 +
                ", ccmV2Configs=" + ccmV2Configs +
                ", tlsStrictCheck=" + tlsStrictCheck +
                '}';
    }
}
