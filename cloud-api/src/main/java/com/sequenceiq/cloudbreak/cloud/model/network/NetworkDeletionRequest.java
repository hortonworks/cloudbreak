package com.sequenceiq.cloudbreak.cloud.model.network;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class NetworkDeletionRequest {

    private final String stackName;

    private final CloudCredential cloudCredential;

    private final String region;

    private final String resourceGroup;

    private final boolean existing;

    private NetworkDeletionRequest(Builder builder) {
        this.stackName = builder.stackName;
        this.cloudCredential = builder.cloudCredential;
        this.region = builder.region;
        this.resourceGroup = builder.resourceGroup;
        this.existing = builder.existing;
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

    public static final class Builder {

        private String stackName;

        private CloudCredential cloudCredential;

        private String region;

        private String resourceGroup;

        private boolean existing;

        public Builder() {
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

        public NetworkDeletionRequest build() {
            return new NetworkDeletionRequest(this);
        }
    }
}
