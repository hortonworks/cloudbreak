package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import javax.validation.constraints.NotEmpty;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

public abstract class ScaleRequestBase {

    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @NotEmpty
    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    @Override
    public String toString() {
        return "ScaleRequestBase{" +
                "environmentCrn='" + environmentCrn + '\'' +
                '}';
    }
}
