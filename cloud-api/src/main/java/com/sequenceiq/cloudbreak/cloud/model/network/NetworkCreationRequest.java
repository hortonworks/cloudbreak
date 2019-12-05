package com.sequenceiq.cloudbreak.cloud.model.network;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;

public class NetworkCreationRequest {

    private final Long envId;

    private final String envName;

    private final CloudCredential cloudCredential;

    private final String variant;

    private final Region region;

    private final String networkCidr;

    private final Set<String> subnetCidrs;

    private final boolean noPublicIp;

    private final String stackName;

    private final boolean privateSubnetEnabled;

    private NetworkCreationRequest(Builder builder) {
        this.envId = builder.envId;
        this.envName = builder.envName;
        this.cloudCredential = builder.cloudCredential;
        this.variant = builder.variant;
        this.region = builder.region;
        this.networkCidr = builder.networkCidr;
        this.subnetCidrs = builder.subnetCidrs;
        this.noPublicIp = builder.noPublicIp;
        this.stackName = builder.stackName;
        this.privateSubnetEnabled = builder.privateSubnetEnabled;
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

    public String getStackName() {
        return stackName;
    }

    public Long getEnvId() {
        return envId;
    }

    public boolean isPrivateSubnetEnabled() {
        return privateSubnetEnabled;
    }

    public static class Builder {
        private Long envId;

        private String envName;

        private CloudCredential cloudCredential;

        private String variant;

        private Region region;

        private String networkCidr;

        private Set<String> subnetCidrs;

        private boolean noPublicIp;

        private String stackName;

        private boolean privateSubnetEnabled;

        public Builder withEnvId(Long envId) {
            this.envId = envId;
            return this;
        }

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

        public Builder withStackName(String stackName) {
            this.stackName = stackName;
            return this;
        }

        public Builder withPrivateSubnetEnabled(boolean privateSubnetEnabled) {
            this.privateSubnetEnabled = privateSubnetEnabled;
            return this;
        }

        public NetworkCreationRequest build() {
            return new NetworkCreationRequest(this);
        }
    }
}
