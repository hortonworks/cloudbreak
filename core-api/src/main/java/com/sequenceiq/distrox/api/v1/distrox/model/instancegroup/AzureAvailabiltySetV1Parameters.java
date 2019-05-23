package com.sequenceiq.distrox.api.v1.distrox.model.instancegroup;

import java.io.Serializable;

import io.swagger.annotations.ApiModelProperty;

public class AzureAvailabiltySetV1Parameters implements Serializable {

    @ApiModelProperty
    private String name;

    @ApiModelProperty
    private Integer faultDomainCount;

    @ApiModelProperty
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
