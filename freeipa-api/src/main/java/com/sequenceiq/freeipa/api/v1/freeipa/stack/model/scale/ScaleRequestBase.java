package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

public abstract class ScaleRequestBase {

    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @NotEmpty
    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    @NotNull
    @Schema(description = ModelDescriptions.AVAILABILITY_TYPE, required = true)
    private AvailabilityType targetAvailabilityType;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public AvailabilityType getTargetAvailabilityType() {
        return targetAvailabilityType;
    }

    public void setTargetAvailabilityType(AvailabilityType targetAvailabilityType) {
        this.targetAvailabilityType = targetAvailabilityType;
    }

    @Override
    public String toString() {
        return "ScaleRequest{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", targetAvailabilityType=" + targetAvailabilityType +
                '}';
    }
}
