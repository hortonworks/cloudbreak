package com.sequenceiq.cloudbreak.structuredevent.event.cdp.redbeams;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RedbeamsDetails implements Serializable {

    private Long resourceId;

    private String name;

    private String resourceCrn;

    private String accountId;

    private String environmentCrn;

    private String region;

    private String availabilityZone;

    public RedbeamsDetails() {
    }

    public RedbeamsDetails(Long resourceId, String name, String resourceCrn, String accountId, String environmentCrn, String region, String availabilityZone) {
        this.resourceId = resourceId;
        this.name = name;
        this.resourceCrn = resourceCrn;
        this.accountId = accountId;
        this.environmentCrn = environmentCrn;
        this.region = region;
        this.availabilityZone = availabilityZone;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }
}
