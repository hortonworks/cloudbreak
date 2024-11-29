package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformSshKeysResponse implements Serializable {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Set<PlatformSshKeyResponse>> sshKeys = new HashMap<>();

    public PlatformSshKeysResponse() {
    }

    public PlatformSshKeysResponse(Map<String, Set<PlatformSshKeyResponse>> sshKeys) {
        this.sshKeys = sshKeys;
    }

    public Map<String, Set<PlatformSshKeyResponse>> getSshKeys() {
        return sshKeys;
    }

    public void setSshKeys(Map<String, Set<PlatformSshKeyResponse>> sshKeys) {
        this.sshKeys = sshKeys;
    }

    @Override
    public String toString() {
        return "PlatformSshKeysResponse{" +
                "sshKeys=" + sshKeys +
                '}';
    }
}
