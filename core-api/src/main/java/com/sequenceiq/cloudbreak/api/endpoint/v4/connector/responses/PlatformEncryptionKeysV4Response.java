package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformEncryptionKeysV4Response implements JsonEntity {

    private Set<EncryptionKeyConfigV4Response> encryptionKeyConfigs = new HashSet<>();

    public Set<EncryptionKeyConfigV4Response> getEncryptionKeyConfigs() {
        return encryptionKeyConfigs;
    }

    public void setEncryptionKeyConfigs(Set<EncryptionKeyConfigV4Response> encryptionKeyConfigs) {
        this.encryptionKeyConfigs = encryptionKeyConfigs;
    }
}
