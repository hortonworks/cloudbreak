package com.sequenceiq.environment.network.dto;

public class GcpParams {

    private final String networkId;

    private final String sharedProjectId;

    private final Boolean noPublicIp;

    private final Boolean noFirewallRules;

    private GcpParams(Builder builder) {
        networkId = builder.networkId;
        sharedProjectId = builder.sharedProjectId;
        noPublicIp = builder.noPublicIp;
        noFirewallRules = builder.noFirewallRules;
    }

    public String getNetworkId() {
        return networkId;
    }

    public String getSharedProjectId() {
        return sharedProjectId;
    }

    public Boolean getNoPublicIp() {
        return noPublicIp;
    }

    public Boolean getNoFirewallRules() {
        return noFirewallRules;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "GcpParams{" +
                "networkId='" + networkId + '\'' +
                ", sharedProjectId='" + sharedProjectId + '\'' +
                ", noFirewallRules='" + noFirewallRules + '\'' +
                ", noPublicIp=" + noPublicIp +
                '}';
    }

    public static final class Builder {

        private String networkId;

        private String sharedProjectId;

        private Boolean noPublicIp;

        private Boolean noFirewallRules;

        private Builder() {
        }

        public Builder withNetworkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        public Builder withSharedProjectId(String sharedProjectId) {
            this.sharedProjectId = sharedProjectId;
            return this;
        }

        public Builder withNoPublicIp(Boolean noPublicIp) {
            this.noPublicIp = noPublicIp;
            return this;
        }

        public Builder withNoFirewallRules(Boolean noFirewallRules) {
            this.noFirewallRules = noFirewallRules;
            return this;
        }

        public GcpParams build() {
            return new GcpParams(this);
        }
    }
}
