package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateInstanceTypeRequest {

    @ValidCrn(resource = {CrnResourceDescriptor.DATAHUB, CrnResourceDescriptor.VM_DATALAKE})
    @NotEmpty
    @ResourceCrn
    @Schema(description = "CRN of the stack to update", requiredMode = Schema.RequiredMode.REQUIRED)
    private String crn;

    @NotEmpty
    @Schema(description = "The target instance type to update to", requiredMode = Schema.RequiredMode.REQUIRED)
    private String instanceType;

    @NotEmpty
    @Schema(description = "The name of the host group to update", requiredMode = Schema.RequiredMode.REQUIRED)
    private String groupName;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public String toString() {
        return "UpdateInstanceTypeRequest{" +
                "crn='" + crn + '\'' +
                ", instanceType='" + instanceType + '\'' +
                ", groupName='" + groupName + '\'' +
                '}';
    }
}
