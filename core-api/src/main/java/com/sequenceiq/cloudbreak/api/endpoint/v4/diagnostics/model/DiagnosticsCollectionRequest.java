package com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.common.api.diagnostics.BaseDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.doc.DiagnosticsModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DiagnosticsCollectionRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiagnosticsCollectionRequest extends BaseDiagnosticsCollectionRequest {

    @NotNull
    @Schema(description = ModelDescriptions.StackModelDescription.CRN)
    private String stackCrn;

    @Schema(description = DiagnosticsModelDescription.UUID)
    private String uuid;

    public String getStackCrn() {
        return stackCrn;
    }

    public void setStackCrn(String stackCrn) {
        this.stackCrn = stackCrn;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
