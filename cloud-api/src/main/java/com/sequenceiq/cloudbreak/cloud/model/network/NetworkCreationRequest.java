package com.sequenceiq.cloudbreak.cloud.model.network;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;

public class NetworkCreationRequest {

    private final Long envId;

    private final String envName;

    private final String envCrn;

    private final CloudCredential cloudCredential;

    private final String variant;

    private final Region region;

    private final String networkCidr;

    private final Set<String> publicSubnetCidrs;

    private final Set<String> privateSubnetCidrs;

    private final boolean noPublicIp;

    private final String stackName;

    private final boolean privateSubnetEnabled;

    private NetworkCreationRequest(Builder builder) {
        envId = builder.envId;
        envName = builder.envName;
        envCrn = builder.envCrn;
        cloudCredential = builder.cloudCredential;
        variant = builder.variant;
        region = builder.region;
        networkCidr = builder.networkCidr;
        publicSubnetCidrs = builder.publicSubnetCidrs;
        privateSubnetCidrs = builder.privateSubnetCidrs;
        noPublicIp = builder.noPublicIp;
        stackName = builder.stackName;
        privateSubnetEnabled = builder.privateSubnetEnabled;
    }

    public String getEnvName() {
        return envName;
    }

    public String getEnvCrn() {
        return envCrn;
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

    public Set<String> getPrivateSubnetCidrs() {
        return privateSubnetCidrs;
    }

    public Set<String> getPublicSubnetCidrs() {
        return publicSubnetCidrs;
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

        private String envCrn;

        private CloudCredential cloudCredential;

        private String variant;

        private Region region;

        private String networkCidr;

        private Set<String> publicSubnetCidrs;

        private Set<String> privateSubnetCidrs;

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

        public Builder withEnvCrn(String envCrn) {
            this.envCrn = envCrn;
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

        public Builder withPublicSubnetCidrs(Set<String> subnetCidrs) {
            this.publicSubnetCidrs = subnetCidrs;
            return this;
        }

        public Builder withPrivateSubnetCidrs(Set<String> subnetCidrs) {
            this.privateSubnetCidrs = subnetCidrs;
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
