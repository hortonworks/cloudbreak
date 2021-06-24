package com.sequenceiq.authorization.utils;

public class EventAuthorizationDto {

    private String resourceCrn;

    private String resourceType;

    public EventAuthorizationDto(String resourceCrn, String resourceType) {
        this.resourceCrn = resourceCrn;
        this.resourceType = resourceType;
    }

    public EventAuthorizationDto() {
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

}
