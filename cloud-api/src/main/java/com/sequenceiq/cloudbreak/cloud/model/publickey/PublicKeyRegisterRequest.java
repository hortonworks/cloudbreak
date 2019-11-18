package com.sequenceiq.cloudbreak.cloud.model.publickey;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.base.RegionAndCredentialAwareRequestBase;

public class PublicKeyRegisterRequest extends RegionAndCredentialAwareRequestBase {

    private @NotNull CloudCredential credential;

    private @NotNull String region;

    private @NotNull String cloudPlatform;

    private @NotNull String publicKeyId;

    private @NotNull String publicKey;

    public PublicKeyRegisterRequest() {
    }

    public PublicKeyRegisterRequest(Builder builder) {
        credential = builder.credential;
        region = builder.region;
        cloudPlatform = builder.cloudPlatform;
        publicKeyId = builder.publicKeyId;
        publicKey = builder.publicKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public String getPublicKeyId() {
        return publicKeyId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    @Override
    public String getRegion() {
        return region;
    }

    @Override
    public CloudCredential getCredential() {
        return credential;
    }

    @Override
    public Platform platform() {
        return Platform.platform(cloudPlatform);
    }

    @Override
    public Variant variant() {
        return Variant.variant(cloudPlatform);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        PublicKeyRegisterRequest that = (PublicKeyRegisterRequest) o;
        return Objects.equals(credential, that.credential) &&
                Objects.equals(region, that.region) &&
                Objects.equals(cloudPlatform, that.cloudPlatform) &&
                Objects.equals(publicKeyId, that.publicKeyId) &&
                Objects.equals(publicKey, that.publicKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(credential, region, cloudPlatform, publicKeyId, publicKey);
    }

    @Override
    public String toString() {
        return "PublicKeyRegisterRequest{" +
                "credential=" + credential +
                ", region='" + region + '\'' +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", publicKeyId='" + publicKeyId + '\'' +
                ", publicKey='" + publicKey + '\'' +
                '}';
    }

    public static class Builder {

        private CloudCredential credential;

        private String region;

        private String cloudPlatform;

        private String publicKeyId;

        private String publicKey;

        private Builder() {
        }

        public Builder withCredential(CloudCredential credential) {
            this.credential = credential;
            return this;
        }

        public Builder withRegion(String region) {
            this.region = region;
            return this;
        }

        public Builder withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public Builder withPublicKeyId(String publicKeyId) {
            this.publicKeyId = publicKeyId;
            return this;
        }

        public Builder withPublicKey(String publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public PublicKeyRegisterRequest build() {
            Objects.requireNonNull(credential);
            Objects.requireNonNull(region);
            Objects.requireNonNull(cloudPlatform);
            Objects.requireNonNull(publicKeyId);
            Objects.requireNonNull(publicKey);
            return new PublicKeyRegisterRequest(this);
        }
    }
}
