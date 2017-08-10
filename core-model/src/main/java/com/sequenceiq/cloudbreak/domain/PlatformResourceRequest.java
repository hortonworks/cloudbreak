package com.sequenceiq.cloudbreak.domain;

import java.util.HashMap;
import java.util.Map;

public class PlatformResourceRequest {

    private Credential credential;

    private String region;

    private String cloudPlatform;

    private String platformVariant;

    private Map<String, String> filters = new HashMap<>();

    public Credential getCredential() {
        return credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getPlatformVariant() {
        return platformVariant;
    }

    public void setPlatformVariant(String platformVariant) {
        this.platformVariant = platformVariant;
    }

    public Map<String, String> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, String> filters) {
        this.filters = filters;
    }
}
