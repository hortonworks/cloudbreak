package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.EncryptionKeyConfigJson;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformEncryptionKeysV4Response implements JsonEntity {

    private Set<EncryptionKeyConfigJson> encryptionKeyConfigs = new HashSet<>();

    public PlatformEncryptionKeysV4Response() {
    }

    public Set<EncryptionKeyConfigJson> getEncryptionKeyConfigs() {
        return encryptionKeyConfigs;
    }

    public void setEncryptionKeyConfigs(Set<EncryptionKeyConfigJson> encryptionKeyConfigs) {
        this.encryptionKeyConfigs = encryptionKeyConfigs;
    }
}
