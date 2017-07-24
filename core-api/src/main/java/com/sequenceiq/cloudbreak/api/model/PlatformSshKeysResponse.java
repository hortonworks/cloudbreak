package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformSshKeysResponse implements JsonEntity {

    private Map<String, Set<PlatformSshKeyResponse>> sshKeys = new HashMap<>();

    public PlatformSshKeysResponse(Map<String, Set<PlatformSshKeyResponse>> sshKeys) {
        this.sshKeys = sshKeys;
    }

    public Map<String, Set<PlatformSshKeyResponse>> getSshKeys() {
        return sshKeys;
    }

    public void setSshKeys(Map<String, Set<PlatformSshKeyResponse>> sshKeys) {
        this.sshKeys = sshKeys;
    }
}
