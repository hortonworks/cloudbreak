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

    public Boolean getCreate() {
        return create;
    }

    public void setCreate(Boolean create) {
        this.create = create;
    }

}
