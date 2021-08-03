package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.List;

public class ConfigRegistrationRequestBuilder {

    private String accountId;

    private final String clusterCrn;

    private String environmentCrn;

    private String uriOfKnox;

    private List<String> aliases;

    private List<ClusterServiceConfig> services;

    private List<String> certificates;

    private boolean useTunnel;

    private List<TunnelEntry> tunnels;

    private boolean useCcmV2;

    private List<CcmV2Config> ccmV2Configs;

    // TODO: cluster-proxy documentation suggests this should be set to "true" in production services. We have not set it ever and the default at
    // cluster-proxy level is "false"
    private boolean tlsStrictCheck;

    public ConfigRegistrationRequestBuilder(String clusterCrn) {
        this.clusterCrn = clusterCrn;
    }

    public ConfigRegistrationRequestBuilder withEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
        return this;
    }

    public ConfigRegistrationRequestBuilder withAliases(List<String> aliases) {
        this.aliases = aliases;
        return this;
    }

    public ConfigRegistrationRequestBuilder withServices(List<ClusterServiceConfig> services) {
        this.services = services;
        return this;
    }

    public ConfigRegistrationRequestBuilder withCertificates(List<String> certificates) {
        this.certificates = certificates;
        return this;
    }

    public ConfigRegistrationRequestBuilder withAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public ConfigRegistrationRequestBuilder withUseCcmV2(boolean useCcmV2) {
        this.useCcmV2 = useCcmV2;
        return this;
    }

    public ConfigRegistrationRequestBuilder withCcmV2Entries(List<CcmV2Config> ccmV2Configs) {
        if (!ccmV2Configs.isEmpty()) {
            this.useCcmV2 = true;
            this.ccmV2Configs = ccmV2Configs;
        }
        return this;
    }

    public ConfigRegistrationRequestBuilder withTunnelEntries(List<TunnelEntry> tunnelEntries) {
        if (tunnelEntries != null && !tunnelEntries.isEmpty()) {
            this.useTunnel = true;
            this.tunnels = tunnelEntries;
        }
        return this;
    }

    public ConfigRegistrationRequestBuilder withKnoxUrl(String knoxUrl) {
        this.uriOfKnox = knoxUrl;
        return this;
    }

    public ConfigRegistrationRequestBuilder withTlsStrictCheck(boolean tlsStrictCheck) {
        this.tlsStrictCheck = tlsStrictCheck;
        return this;
    }

    public ConfigRegistrationRequest build() {
        return new ConfigRegistrationRequest(clusterCrn, environmentCrn, uriOfKnox, accountId,
                useTunnel, tunnels, aliases, services, certificates, useCcmV2, ccmV2Configs, tlsStrictCheck);
    }
}
