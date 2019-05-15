package com.sequenceiq.environment.api.platformresource.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformEncryptionKeysV1Response implements Serializable {

    private Set<EncryptionKeyConfigV1Response> encryptionKeyConfigs = new HashSet<>();

    public Set<EncryptionKeyConfigV1Response> getEncryptionKeyConfigs() {
        return encryptionKeyConfigs;
    }

    public void setEncryptionKeyConfigs(Set<EncryptionKeyConfigV1Response> encryptionKeyConfigs) {
        this.encryptionKeyConfigs = encryptionKeyConfigs;
    }
}
