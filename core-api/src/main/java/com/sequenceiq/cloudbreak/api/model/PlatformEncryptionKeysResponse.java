package com.sequenceiq.cloudbreak.api.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformEncryptionKeysResponse implements JsonEntity {

    private Set<EncryptionKeyConfigJson> encryptionKeyConfigs = new HashSet<>();

    public Set<EncryptionKeyConfigJson> getEncryptionKeyConfigs() {
        return encryptionKeyConfigs;
    }

    public void setEncryptionKeyConfigs(Set<EncryptionKeyConfigJson> encryptionKeyConfigs) {
        this.encryptionKeyConfigs = encryptionKeyConfigs;
    }
}
