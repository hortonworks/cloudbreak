package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import javax.validation.constraints.NotEmpty;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.FormFactor;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

public abstract class ScaleRequestBase {

    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @NotEmpty
    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    @ApiModelProperty(value = ModelDescriptions.FORM_FACTOR, required = true)
    private FormFactor targetFormFactor;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public FormFactor getTargetFormFactor() {
        return targetFormFactor;
    }

    public void setTargetFormFactor(FormFactor targetFormFactor) {
        this.targetFormFactor = targetFormFactor;
    }

    @Override
    public String toString() {
        return "ScaleRequest{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", targetFormFactor=" + targetFormFactor +
                '}';
    }
}
