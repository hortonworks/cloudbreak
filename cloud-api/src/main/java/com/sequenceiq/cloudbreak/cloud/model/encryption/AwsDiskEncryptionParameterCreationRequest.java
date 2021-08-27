package com.sequenceiq.cloudbreak.cloud.model.encryption;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

public class AwsDiskEncryptionParameterCreationRequest implements CloudPlatformAware {

    private final String encryptionKeyArn;

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private AwsDiskEncryptionParameterCreationRequest(Builder builder) {
        this.encryptionKeyArn = builder.encryptionKeyArn;
        this.cloudContext = builder.cloudContext;
        this.cloudCredential = builder.cloudCredential;
    }

    public String getEncryptionKeyArn() {
        return encryptionKeyArn;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public  CloudCredential getCloudCredential() { return  cloudCredential; }

    @Override
    public Platform platform() {
        return cloudContext.getPlatform();
    }

    @Override
    public Variant variant() {
        return cloudContext.getVariant();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AwsDiskEncryptionParameterCreationRequest{");
        sb.append("encryptionKeyArn='").append(encryptionKeyArn).append('\'');
        sb.append(", cloudContext=").append(cloudContext);
        sb.append(", cloudCredential=").append(cloudCredential);
        sb.append('}');
        return sb.toString();
    }

    public static final class Builder {

        private CloudContext cloudContext;

        private CloudCredential cloudCredential;

        private String encryptionKeyArn;

        public Builder() {
        }

        public Builder withCloudContext(CloudContext cloudContext) {
            this.cloudContext = cloudContext;
            return this;
        }

        public Builder withCloudCredential(CloudCredential cloudCredential) {
            this.cloudCredential = cloudCredential;
            return this;
        }

        public Builder withEncryptionKeyArn(String encryptionKeyArn) {
            this.encryptionKeyArn = encryptionKeyArn;
            return this;
        }

        public AwsDiskEncryptionParameterCreationRequest build() {
            return new AwsDiskEncryptionParameterCreationRequest(this);
        }

    }

}