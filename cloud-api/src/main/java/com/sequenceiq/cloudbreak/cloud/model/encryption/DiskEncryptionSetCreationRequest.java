package com.sequenceiq.cloudbreak.cloud.model.encryption;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

public class DiskEncryptionSetCreationRequest implements CloudPlatformAware {

    private final String id;

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final String resourceGroupName;

    private final boolean singleResourceGroup;

    private final Map<String, String> tags;

    private final String encryptionKeyUrl;

    private DiskEncryptionSetCreationRequest(Builder builder) {
        this.id = builder.id;
        this.cloudCredential = builder.cloudCredential;
        this.resourceGroupName = builder.resourceGroupName;
        this.singleResourceGroup = builder.singleResourceGroup;
        this.tags = builder.tags;
        this.encryptionKeyUrl = builder.encryptionKeyUrl;
        this.cloudContext = builder.cloudContext;
    }

    public String getId() {
        return id;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public boolean isSingleResourceGroup() {
        return singleResourceGroup;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public String getEncryptionKeyUrl() {
        return encryptionKeyUrl;
    }

    @Override
    public Platform platform() {
        return cloudContext.getPlatform();
    }

    @Override
    public Variant variant() {
        return cloudContext.getVariant();
    }

    // Must not reveal any secrets, hence not including encryptionKeyUrl!
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DiskEncryptionSetCreationRequest{");
        sb.append("id='").append(id).append('\'');
        sb.append(", cloudContext=").append(cloudContext);
        sb.append(", cloudCredential=").append(cloudCredential);
        sb.append(", resourceGroupName='").append(resourceGroupName).append('\'');
        sb.append(", singleResourceGroup=").append(singleResourceGroup);
        sb.append(", tags=").append(tags);
        sb.append('}');
        return sb.toString();
    }

    public static final class Builder {

        private String id;

        private CloudContext cloudContext;

        private CloudCredential cloudCredential;

        private String resourceGroupName;

        private boolean singleResourceGroup;

        private Map<String, String> tags = new HashMap<>();

        private String encryptionKeyUrl;

        public Builder() {
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withCloudContext(CloudContext cloudContext) {
            this.cloudContext = cloudContext;
            return this;
        }

        public Builder withCloudCredential(CloudCredential cloudCredential) {
            this.cloudCredential = cloudCredential;
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

        public DiskEncryptionSetCreationRequest build() {
            return new DiskEncryptionSetCreationRequest(this);
        }

    }

}