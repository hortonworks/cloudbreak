package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class DistroXOperationValidationResponse {

    @Schema(description = ModelDescriptions.StackModelDescription.DISTROX_OPERATION, requiredMode = Schema.RequiredMode.REQUIRED)
    private String operation;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean allowed;

    @Schema(description = ModelDescriptions.StackModelDescription.SDX_STATUS_REASON)
    private String reason;

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "DistroXOperationValidationResponse{" +
                "operation='" + operation + '\'' +
                ", allowed=" + allowed +
                ", reason='" + reason + '\'' +
                '}';
    }
}
