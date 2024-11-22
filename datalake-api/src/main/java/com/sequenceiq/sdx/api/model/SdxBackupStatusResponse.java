package com.sequenceiq.sdx.api.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxBackupStatusResponse {

    @Schema(description = ModelDescriptions.OPERATION_ID)
    private String operationId;

    @Schema(description = ModelDescriptions.OPERATION_STATUS)
    private String status;

    @Schema(description = ModelDescriptions.OPERATION_STATUS_REASON)
    private String reason;

    @Schema(description = ModelDescriptions.BACKUP_INCLUDED_DATA, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> includedData = new ArrayList<>();

    @Schema(description = ModelDescriptions.BACKUP_TIMESTAMP)
    private String timestamp;

    @Schema(description = ModelDescriptions.BACKUP_END_TIMESTAMP)
    private String endTimestamp;

    public SdxBackupStatusResponse() {
    }

    public SdxBackupStatusResponse(String operationId, String status, String reason, List<String> includedData, String timestamp, String endTimestamp) {
        this.operationId = operationId;
        this.status = status;
        this.reason = reason;
        this.includedData = includedData;
        this.timestamp = timestamp;
        this.endTimestamp = endTimestamp;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public List<String> getIncludedData() {
        return includedData;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getEndTimestamp() {
        return endTimestamp;
    }

    @Override
    public String toString() {
        return "SdxBackupStatusResponse{" + "OperationId=" + operationId + "Status=" + status + "Reason=" + reason + '}';
    }
}
