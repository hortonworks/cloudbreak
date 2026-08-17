package com.sequenceiq.maintenance.api.v1.schedule.model.request;

import jakarta.validation.constraints.Size;

import com.sequenceiq.maintenance.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.SkipRequest.REQUEST)
public class MaintenanceWindowSkipRequest {

    @Size(max = 1024)
    @Schema(description = ModelDescriptions.SkipRequest.REASON)
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "MaintenanceWindowSkipRequest{" +
                "reason='" + reason + '\'' +
                '}';
    }
}
