package com.sequenceiq.cloudbreak.cloud.model.encryption;

import java.util.Map;

public class CreatedEncryptionResources {
    private String encryptionResourceId;

    private String encryptionResourcePrincipalId;

    private String encryptionResourceLocation;

    private String encryptionResourceName;

    private Map<String, String> tags;

    public CreatedEncryptionResources(String encryptionResourceId, String encryptionResourcePrincipalId,
            String encryptionResourceLocation, Map<String, String> tags, String encryptionResourceName) {
        this.encryptionResourceId = encryptionResourceId;
        this.encryptionResourcePrincipalId = encryptionResourcePrincipalId;
        this.encryptionResourceLocation = encryptionResourceLocation;
        this.encryptionResourceName = encryptionResourceName;
        this.tags = tags;
    }

    public String getEncryptionResourceId() {
        return encryptionResourceId;
    }

    public String getEncryptionResourcePrincipalId() {
        return encryptionResourcePrincipalId;
    }

    public String getEncryptionResourceLocation() {
        return encryptionResourceLocation;
    }

    public String getEncryptionResourceName() {
        return encryptionResourceName;
    }

    public Map<String, String> getTags() {
        return tags;
    }
}