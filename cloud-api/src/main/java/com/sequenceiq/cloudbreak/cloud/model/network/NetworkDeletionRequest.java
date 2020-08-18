package com.sequenceiq.cloudbreak.cloud.model.network;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class NetworkDeletionRequest {

    private final String stackName;

    private final CloudCredential cloudCredential;

    private final String region;

    private final String resourceGroup;

    private final boolean existing;

    private final Set<String> subnetIds;

    private final String networkId;

    private final Long envId;

    private final String envName;

    private final String userId;

    private final String accountId;

    private NetworkDeletionRequest(Builder builder) {
        this.stackName = builder.stackName;
        this.cloudCredential = builder.cloudCredential;
        this.region = builder.region;
        this.resourceGroup = builder.resourceGroup;
        this.existing = builder.existing;
        this.subnetIds = builder.subnetIds;
        this.networkId = builder.networkId;
        this.envId = builder.envId;
        this.envName = builder.envName;
        this.userId = builder.userId;
        this.accountId = builder.accountId;
    }

    public String getStackName() {
        return stackName;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public String getRegion() {
        return region;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public boolean isExisting() {
        return existing;
    }

    public Set<String> getSubnetIds() {
        return subnetIds;
    }

    public String getNetworkId() {
        return networkId;
    }

    public Long getEnvId() {
        return envId;
    }

    public String getEnvName() {
        return envName;
    }

    public String getUserId() {
        return userId;
    }

    public String getAccountId() {
        return accountId;
    }

    public static final class Builder {

        private String stackName;

        private CloudCredential cloudCredential;

        private String region;

        private String resourceGroup;

        private boolean existing;

        private Set<String> subnetIds;

        private String networkId;

        private Long envId;

        private String envName;

        private String userId;

        private String accountId;

        public Builder() {
        }


        public Builder withUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }


        public Builder withEnvId(Long envId) {
            this.envId = envId;
            return this;
        }

        public Builder withEnvName(String envName) {
            this.envName = envName;
            return this;
        }

        public Builder withStackName(String stackName) {
            this.stackName = stackName;
            return this;
        }

        public Builder withCloudCredential(CloudCredential cloudCredential) {
            this.cloudCredential = cloudCredential;
            return this;
        }

        public Builder withRegion(String region) {
            this.region = region;
            return this;
        }

        public Builder withResourceGroup(String resourceGroup) {
            this.resourceGroup = resourceGroup;
            return this;
        }

        public Builder withExisting(boolean existing) {
            this.existing = existing;
            return this;
        }

        public Builder withNetworkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        public Builder withSubnetIds(Set<String> subnetIds) {
            this.subnetIds = subnetIds;
            return this;
        }

        public NetworkDeletionRequest build() {
            return new NetworkDeletionRequest(this);
        }
    }
}