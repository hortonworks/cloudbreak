package com.sequenceiq.environment.platformresource;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.environment.credential.domain.Credential;

public class PlatformResourceRequest {

    private Credential credential;

    private String region;

    private String cloudPlatform;

    private String platformVariant;

    private String availabilityZone;

    private CdpResourceType cdpResourceType = CdpResourceType.DEFAULT;

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

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public CdpResourceType getCdpResourceType() {
        return cdpResourceType;
    }

    public void setCdpResourceType(CdpResourceType cdpResourceType) {
        this.cdpResourceType = cdpResourceType;
    }

    @Override
    public String toString() {
        return "PlatformResourceRequest{" +
                "credential=" + credential +
                ", region='" + region + '\'' +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", platformVariant='" + platformVariant + '\'' +
                ", availabilityZone='" + availabilityZone + '\'' +
                ", cdpResourceType=" + cdpResourceType +
                ", filters=" + filters +
                '}';
    }
}
