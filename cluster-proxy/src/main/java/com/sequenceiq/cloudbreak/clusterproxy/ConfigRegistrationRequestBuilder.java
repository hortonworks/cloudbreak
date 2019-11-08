package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.List;

public class ConfigRegistrationRequestBuilder {
    private String accountId;

    private String clusterCrn;

    private String uriOfKnox;

    private List<String> aliases;

    private List<ClusterServiceConfig> services;

    private List<String> certificates;

    private boolean useTunnel;

    private List<TunnelEntry> tunnels;

    public ConfigRegistrationRequestBuilder(String clusterCrn) {
        this.clusterCrn = clusterCrn;
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

    public ConfigRegistrationRequestBuilder withTunnelEntries(List<TunnelEntry> tunnelEntries) {
        this.useTunnel = true;
        this.tunnels = tunnelEntries;
        return this;
    }

    public ConfigRegistrationRequestBuilder withKnoxUrl(String knoxUrl) {
        this.uriOfKnox = knoxUrl;
        return this;
    }

    public ConfigRegistrationRequest build() {
        return new ConfigRegistrationRequest(clusterCrn, uriOfKnox, accountId, useTunnel, tunnels, aliases, services, certificates);
    }
}
