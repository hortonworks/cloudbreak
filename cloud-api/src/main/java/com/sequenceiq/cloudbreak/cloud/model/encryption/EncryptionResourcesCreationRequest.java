package com.sequenceiq.cloudbreak.cloud.model.encryption;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class EncryptionResourcesCreationRequest {

    private final CloudCredential cloudCredential;

    private final String region;

    private final String resourceGroupName;

    private final boolean singleResourceGroup;

    private final Long envId;

    private final String envName;

    private final Map<String, String> tags;

    private final String encryptionKeyUrl;

    private EncryptionResourcesCreationRequest(Builder builder) {
        this.cloudCredential = builder.cloudCredential;
        this.region = builder.region;
        this.resourceGroupName = builder.resourceGroupName;
        this.singleResourceGroup = builder.singleResourceGroup;
        this.envId = builder.envId;
        this.envName = builder.envName;
        this.tags = builder.tags;
        this.encryptionKeyUrl = builder.encryptionKeyUrl;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public String getRegion() {
        return region;
    }

    public String getResourceGroup() {
        return resourceGroupName;
    }

    public boolean isSingleResourceGroup() {
        return singleResourceGroup;
    }

    public Long getEnvId() {
        return envId;
    }

    public String getEnvName() {
        return envName;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public String getEncryptionKeyUrl() {
        return encryptionKeyUrl;
    }

    public static final class Builder {

        private CloudCredential cloudCredential;

        private String region;

        private String resourceGroupName;

        private boolean singleResourceGroup;

        private Long envId;

        private String envName;

        private Map<String, String> tags = new HashMap<>();

        private String encryptionKeyUrl;

        public Builder() {
        }

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

        public Builder withRegion(String region) {
            this.region = region;
            return this;
        }

        public Builder withResourceGroupName(String resourceGroupName) {
            this.resourceGroupName = resourceGroupName;
            return this;
        }

        public Builder withSingleResourceGroup(boolean singleResourceGroup) {
            this.singleResourceGroup = singleResourceGroup;
            return this;
        }

        public Builder withTags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder withEncryptionKeyUrl(String encryptionKeyUrl) {
            this.encryptionKeyUrl = encryptionKeyUrl;
            return this;
        }

        public EncryptionResourcesCreationRequest build() {
            return new EncryptionResourcesCreationRequest(this);
        }
    }
}