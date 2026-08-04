package com.sequenceiq.freeipa.api.v1.freeipa.stack.model;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UpdateInstanceTypeV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateInstanceTypeRequest {

    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @NotEmpty
    @ResourceCrn
    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String environmentCrn;

    @NotEmpty
    @Schema(description = "The target instance type to update to", requiredMode = Schema.RequiredMode.REQUIRED)
    private String instanceType;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    @Override
    public String toString() {
        return "UpdateInstanceTypeRequest{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", instanceType='" + instanceType + '\'' +
                '}';
    }
}
