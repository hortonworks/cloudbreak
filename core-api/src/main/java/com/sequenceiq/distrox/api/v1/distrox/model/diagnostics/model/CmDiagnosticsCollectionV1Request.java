package com.sequenceiq.distrox.api.v1.distrox.model.diagnostics.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.common.api.diagnostics.BaseCmDiagnosticsCollectionRequest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("CmDiagnosticsCollectionV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CmDiagnosticsCollectionV1Request extends BaseCmDiagnosticsCollectionRequest {

    @NotNull
    @ApiModelProperty(ModelDescriptions.StackModelDescription.CRN)
    private String stackCrn;

    @Override
    public String getStackCrn() {
        return stackCrn;
    }

    @Override
    public void setStackCrn(String stackCrn) {
        this.stackCrn = stackCrn;
    }
}
