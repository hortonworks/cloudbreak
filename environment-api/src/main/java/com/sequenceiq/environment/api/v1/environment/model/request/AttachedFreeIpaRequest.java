package com.sequenceiq.environment.api.v1.environment.model.request;

import javax.validation.constraints.NotNull;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "AttachedFreeIpaRequest")
public class AttachedFreeIpaRequest {

    @NotNull
    @ApiModelProperty(value = EnvironmentModelDescription.CREATE_FREEIPA, required = true)
    private Boolean create;

    @ApiModelProperty(value = EnvironmentModelDescription.FREEIPA_INSTANCE_COUNT_BY_GROUP)
    private Integer instanceCountByGroup;

    public Boolean getCreate() {
        return create;
    }

    public void setCreate(Boolean create) {
        this.create = create;
    }

    public Integer getInstanceCountByGroup() {
        return instanceCountByGroup;
    }

    public void setInstanceCountByGroup(Integer instanceCountByGroup) {
        this.instanceCountByGroup = instanceCountByGroup;
    }

}
