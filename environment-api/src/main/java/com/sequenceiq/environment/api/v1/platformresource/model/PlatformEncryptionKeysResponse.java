package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformEncryptionKeysResponse implements Serializable {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<EncryptionKeyConfigResponse> encryptionKeyConfigs = new HashSet<>();

    public Set<EncryptionKeyConfigResponse> getEncryptionKeyConfigs() {
        return encryptionKeyConfigs;
    }

    public void setEncryptionKeyConfigs(Set<EncryptionKeyConfigResponse> encryptionKeyConfigs) {
        this.encryptionKeyConfigs = encryptionKeyConfigs;
    }

    @Override
    public String toString() {
        return "PlatformEncryptionKeysResponse{" +
                "encryptionKeyConfigs=" + encryptionKeyConfigs +
                '}';
    }
}
