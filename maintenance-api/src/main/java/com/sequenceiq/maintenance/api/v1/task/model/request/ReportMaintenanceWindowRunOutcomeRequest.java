package com.sequenceiq.maintenance.api.v1.task.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.sequenceiq.maintenance.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.RunOutcomeRequest.REQUEST)
public class ReportMaintenanceWindowRunOutcomeRequest {

    @NotBlank
    @Schema(description = ModelDescriptions.RunOutcomeRequest.STATUS, requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @Size(max = 1024)
    @Schema(description = ModelDescriptions.RunOutcomeRequest.ERROR_DETAIL)
    private String errorDetail;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }

    @Override
    public String toString() {
        return "ReportMaintenanceWindowRunOutcomeRequest{" +
                "status='" + status + '\'' +
                ", errorDetail='" + errorDetail + '\'' +
                '}';
    }
}
