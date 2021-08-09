package com.sequenceiq.environment.api.v1.environment.model.request.gcp;

import java.io.Serializable;

import javax.validation.constraints.Pattern;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "GcpResourceEncryptionV1Parameters")
public class GcpResourceEncryptionParameters implements Serializable {

    @VisibleForTesting
    static final String ENCRYPTION_KEY_INVALID_MSG =
            "Expected Format: '/projects/<projectName>/locations/<location>/keyRings/<KeyRing>/cryptoKeys/<KeyName>'. " +
            "Key location should be same as resource location " +
            "<keyName> may only contain alphanumeric characters and dashes.";

    @ApiModelProperty(EnvironmentModelDescription.ENCRYPTION_KEY)
    @Pattern(regexp = "projects\\/[a-zA-Z0-9_-]{1,63}\\/" +
            "locations\\/[a-zA-Z0-9_-]{1,63}\\/keyRings\\/[a-zA-Z0-9_-]{1,63}\\/cryptoKeys\\/[a-zA-Z0-9_-]{1,63}",
            message = ENCRYPTION_KEY_INVALID_MSG)
    private String encryptionKey;

    public GcpResourceEncryptionParameters() {
    }

    private GcpResourceEncryptionParameters(GcpResourceEncryptionParameters.Builder builder) {
        this.encryptionKey = builder.encryptionKey;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "GcpResourceEncryptionParameters{" +
                "encryptionKey='" + encryptionKey + '\'' +
                '}';
    }

    public static class Builder {
        private String encryptionKey;

        private Builder() {
        }

        public Builder withEncryptionKey(String encryptionKey) {
            this.encryptionKey = encryptionKey;
            return this;
        }

        public GcpResourceEncryptionParameters build() {
            return new GcpResourceEncryptionParameters(this);
        }
    }
}