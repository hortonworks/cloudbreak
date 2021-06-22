package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.detachchildenv;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("DetachChildEnvironmentV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DetachChildEnvironmentRequest {

    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.PARENT_ENVIRONMENT_CRN, required = true)
    private String parentEnvironmentCrn;

    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.CHILD_ENVIRONMENT_CRN, required = true)
    private String childEnvironmentCrn;

    public String getParentEnvironmentCrn() {
        return parentEnvironmentCrn;
    }

    public void setParentEnvironmentCrn(String parentEnvironmentCrn) {
        this.parentEnvironmentCrn = parentEnvironmentCrn;
    }

    public String getChildEnvironmentCrn() {
        return childEnvironmentCrn;
    }

    public void setChildEnvironmentCrn(String childEnvironmentCrn) {
        this.childEnvironmentCrn = childEnvironmentCrn;
    }

    @Override
    public String toString() {
        return "DetachChildEnvironmentRequest{"
                + "parentEnvironmentCrn='" + parentEnvironmentCrn + '\''
                + "childEnvironmentCrn='" + childEnvironmentCrn + '\''
                + '}';
    }
}
