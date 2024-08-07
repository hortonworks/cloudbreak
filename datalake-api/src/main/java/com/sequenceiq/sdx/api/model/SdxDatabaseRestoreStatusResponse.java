package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxDatabaseRestoreStatusResponse {

    @Schema(description = ModelDescriptions.OPERATION_STATUS)
    private DatalakeDatabaseDrStatus status;

    @Schema(description = ModelDescriptions.OPERATION_STATUS_REASON)
    private String statusReason;

    public SdxDatabaseRestoreStatusResponse(DatalakeDatabaseDrStatus status) {
        this.status = status;
    }

    public SdxDatabaseRestoreStatusResponse(DatalakeDatabaseDrStatus status, String statusReason) {
        this.status = status;
        this.statusReason = statusReason;
    }

    public DatalakeDatabaseDrStatus getStatus() {
        return status;
    }

    public String getStatusReason() {
        return statusReason;
    }
}
