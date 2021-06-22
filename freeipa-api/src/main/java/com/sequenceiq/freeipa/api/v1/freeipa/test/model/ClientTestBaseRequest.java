package com.sequenceiq.freeipa.api.v1.freeipa.test.model;

import javax.validation.constraints.NotEmpty;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

public abstract class ClientTestBaseRequest {

    @NotEmpty
    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }
}
