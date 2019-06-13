package com.sequenceiq.cloudbreak.cloud.model.network;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;

public class NetworkCreationRequest {

    private final String envName;

    private final CloudCredential cloudCredential;

    private final String variant;

    private final Region region;

    private final String networkCidr;

    private final Set<String> subnetCidrs;

    private final boolean noPublicIp;

    private final boolean noFirewallRules;

    private NetworkCreationRequest(Builder builder) {
        this.envName = builder.envName;
        this.cloudCredential = builder.cloudCredential;
        this.variant = builder.variant;
        this.region = builder.region;
        this.networkCidr = builder.networkCidr;
        this.subnetCidrs = builder.subnetCidrs;
        this.noPublicIp = builder.noPublicIp;
        this.noFirewallRules = builder.noFirewallRules;
    }

    public String getEnvName() {
        return envName;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public String getVariant() {
        return variant;
    }

    public Region getRegion() {
        return region;
    }

    public String getNetworkCidr() {
        return networkCidr;
    }

    public Set<String> getSubnetCidrs() {
        return subnetCidrs;
    }

    public boolean isNoPublicIp() {
        return noPublicIp;
    }

    public boolean isNoFirewallRules() {
        return noFirewallRules;
    }

    public static class Builder {
        private String envName;

        private CloudCredential cloudCredential;

        private String variant;

        private Region region;

        private String networkCidr;

        private Set<String> subnetCidrs;

        private boolean noPublicIp;

        private boolean noFirewallRules;

        public Builder withEnvName(String envName) {
            this.envName = envName;
            return this;
        }

        public Builder withCloudCredential(CloudCredential cloudCredential) {
            this.cloudCredential = cloudCredential;
            return this;
        }

        public Builder withVariant(String variant) {
            this.variant = variant;
            return this;
        }

        public Builder withRegion(Region region) {
            this.region = region;
            return this;
        }

        public Builder withNetworkCidr(String networkCidr) {
            this.networkCidr = networkCidr;
            return this;
        }

        public Builder withSubnetCidrs(Set<String> subnetCidrs) {
            this.subnetCidrs = subnetCidrs;
            return this;
        }

        public Builder withNoPublicIp(boolean noPublicIp) {
            this.noPublicIp = noPublicIp;
            return this;
        }

        public Builder withNoFirewallRules(boolean noFirewallRules) {
            this.noFirewallRules = noFirewallRules;
            return this;
        }

        public NetworkCreationRequest build() {
            return new NetworkCreationRequest(this);
        }
    }
}
