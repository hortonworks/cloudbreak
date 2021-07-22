package com.sequenceiq.environment.api.v1.environment.model.request.gcp;

import javax.validation.constraints.Pattern;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "GcpResourceEncryptionV1Parameters")
public class GcpResourceEncryptionParameters {

    @VisibleForTesting
    static final String ENCRYPTION_KEY_INVALID_MSG =
            "Expected Format: '/projects/<projectName>/locations/<location>/keyRings/<KeyRing>/cryptoKeys/<Key name>'. " +
            "Key location should be same as resource location " +
            "<keyName> may only contain alphanumeric characters and dashes. ";

    @ApiModelProperty(EnvironmentModelDescription.ENCRYPTION_KEY)
    @Pattern(regexp =
            "/projects/[a-zA-Z-][0-9a-zA-Z-]*/locations/[a-zA-Z-][0-9a-zA-Z-]*/keyRings/[a-zA-Z-][0-9a-zA-Z-]*/cryptoKeys/[a-zA-Z-][0-9a-zA-Z-]*",
            message = ENCRYPTION_KEY_INVALID_MSG)
    private String encryptionKey;

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

        public Builder withEncryptionKey(String encryptionKeyResource) {
            this.encryptionKey = encryptionKeyResource;
            return this;
        }

        public GcpResourceEncryptionParameters build() {
            GcpResourceEncryptionParameters resourceEncryptionParameters = new GcpResourceEncryptionParameters();
            resourceEncryptionParameters.setEncryptionKey(encryptionKey);
            return resourceEncryptionParameters;
        }
    }
}
