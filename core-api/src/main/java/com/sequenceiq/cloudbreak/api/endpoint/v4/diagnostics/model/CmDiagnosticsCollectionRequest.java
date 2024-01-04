package com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.common.api.diagnostics.BaseCmDiagnosticsCollectionRequest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class CmDiagnosticsCollectionRequest extends BaseCmDiagnosticsCollectionRequest {

    @NotNull
    @Schema(description = ModelDescriptions.StackModelDescription.CRN)
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
