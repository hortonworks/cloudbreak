package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rebuild;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.service.api.doc.ModelDescriptions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@ApiModel("RebuildV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RebuildRequest {
    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @NotEmpty
    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.REBUILD_SOURCE_CRN, required = true)
    private String sourceCrn;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getSourceCrn() {
        return sourceCrn;
    }

    public void setSourceCrn(String sourceCrn) {
        this.sourceCrn = sourceCrn;
    }

    @Override
    public String toString() {
        return "RebuildRequest{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", sourceCrn='" + sourceCrn + '\'' +
                '}';
    }
}
