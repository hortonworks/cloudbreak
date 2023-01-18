package com.sequenceiq.distrox.api.v1.distrox.model.diagnostics.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.common.api.diagnostics.BaseDiagnosticsCollectionRequest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DiagnosticsCollectionV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiagnosticsCollectionV1Request extends BaseDiagnosticsCollectionRequest {

    @NotNull
    @Schema(description = ModelDescriptions.StackModelDescription.CRN)
    private String stackCrn;

    public String getStackCrn() {
        return stackCrn;
    }

    public void setStackCrn(String stackCrn) {
        this.stackCrn = stackCrn;
    }
}
