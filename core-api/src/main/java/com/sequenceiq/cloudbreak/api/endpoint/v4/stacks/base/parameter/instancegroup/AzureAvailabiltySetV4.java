package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup;

import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModelProperty;

public class AzureAvailabiltySetV4 implements JsonEntity {

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
