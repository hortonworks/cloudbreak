package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureAvailabiltySetV4 implements JsonEntity {

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
