package com.sequenceiq.environment.api.v1.environment.model.request.gcp;

import java.io.Serializable;

import javax.validation.constraints.Pattern;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.S3GuardRequestParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "GcpResourceEncryptionV1Parameters")
public class GcpResourceEncryptionParameters implements Serializable {

    @VisibleForTesting
    static final String ENCRYPTION_KEY_INVALID_MSG =
            "Expected Format: '/projects/<projectName>/locations/<location>/keyRings/<KeyRing>/cryptoKeys/<Key name>/cryptoKeyVersions/<version>'. " +
            "Key location should be same as resource location " +
            "<keyName> may only contain alphanumeric characters and dashes.";

    @ApiModelProperty(EnvironmentModelDescription.ENCRYPTION_KEY)
    @Pattern(regexp = ".*",
            message = ENCRYPTION_KEY_INVALID_MSG)
    private String encryptionKey;

    public GcpResourceEncryptionParameters(){
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
/*            GcpResourceEncryptionParameters resourceEncryptionParameters = new GcpResourceEncryptionParameters();
            resourceEncryptionParameters.setEncryptionKey(encryptionKey);
            return resourceEncryptionParameters;*/
            return new GcpResourceEncryptionParameters(this);
        }
    }
}
