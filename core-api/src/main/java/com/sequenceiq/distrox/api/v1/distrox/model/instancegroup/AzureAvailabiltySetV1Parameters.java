package com.sequenceiq.distrox.api.v1.distrox.model.instancegroup;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;

public class AzureAvailabiltySetV1Parameters implements Serializable {

    @Schema
    private String name;

    @Schema
    private Integer faultDomainCount;

    @Schema
    private Integer updateDomainCount;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getFaultDomainCount() {
        return faultDomainCount;
    }

    public void setFaultDomainCount(Integer faultDomainCount) {
        this.faultDomainCount = faultDomainCount;
    }

    public Integer getUpdateDomainCount() {
        return updateDomainCount;
    }

    public void setUpdateDomainCount(Integer updateDomainCount) {
        this.updateDomainCount = updateDomainCount;
    }
}
