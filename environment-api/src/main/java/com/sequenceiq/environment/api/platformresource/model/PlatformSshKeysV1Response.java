package com.sequenceiq.environment.api.platformresource.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformSshKeysV1Response implements Serializable {

    private Map<String, Set<PlatformSshKeyV1Response>> sshKeys = new HashMap<>();

    public PlatformSshKeysV1Response() {
    }

    public PlatformSshKeysV1Response(Map<String, Set<PlatformSshKeyV1Response>> sshKeys) {
        this.sshKeys = sshKeys;
    }

    public Map<String, Set<PlatformSshKeyV1Response>> getSshKeys() {
        return sshKeys;
    }

    public void setSshKeys(Map<String, Set<PlatformSshKeyV1Response>> sshKeys) {
        this.sshKeys = sshKeys;
    }
}
