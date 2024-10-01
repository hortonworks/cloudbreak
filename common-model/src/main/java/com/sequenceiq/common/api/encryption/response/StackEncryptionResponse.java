package com.sequenceiq.common.api.encryption.response;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.encryption.doc.StackEncryptionModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "StackEncryptionV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StackEncryptionResponse implements Serializable {

    @Schema(description = StackEncryptionModelDescription.ENCRYPTION_KEY_LUKS)
    private String encryptionKeyLuks;

    @Schema(description = StackEncryptionModelDescription.ENCRYPTION_KEY_CLOUD_SECRET_MANAGER)
    private String encryptionKeyCloudSecretManager;

    public String getEncryptionKeyLuks() {
        return encryptionKeyLuks;
    }

    public void setEncryptionKeyLuks(String encryptionKeyLuks) {
        this.encryptionKeyLuks = encryptionKeyLuks;
    }

    public String getEncryptionKeyCloudSecretManager() {
        return encryptionKeyCloudSecretManager;
    }

    public void setEncryptionKeyCloudSecretManager(String encryptionKeyCloudSecretManager) {
        this.encryptionKeyCloudSecretManager = encryptionKeyCloudSecretManager;
    }

    @Override
    public String toString() {
        return "StackEncryptionResponse{" +
                "encryptionKeyLuks='" + encryptionKeyLuks + '\'' +
                ", encryptionKeyCloudSecretManager='" + encryptionKeyCloudSecretManager + '\'' +
                '}';
    }
}
