package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformSshKeysV4Response implements JsonEntity {

    private Map<String, Set<PlatformSshKeyV4Response>> sshKeys = new HashMap<>();

    public PlatformSshKeysV4Response() {
    }

    public PlatformSshKeysV4Response(Map<String, Set<PlatformSshKeyV4Response>> sshKeys) {
        this.sshKeys = sshKeys;
    }

    public Map<String, Set<PlatformSshKeyV4Response>> getSshKeys() {
        return sshKeys;
    }

    public void setSshKeys(Map<String, Set<PlatformSshKeyV4Response>> sshKeys) {
        this.sshKeys = sshKeys;
    }
}
