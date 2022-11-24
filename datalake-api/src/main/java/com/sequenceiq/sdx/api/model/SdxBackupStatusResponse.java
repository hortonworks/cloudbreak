package com.sequenceiq.sdx.api.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxBackupStatusResponse {

    @ApiModelProperty(ModelDescriptions.OPERATION_ID)
    private String operationId;

    @ApiModelProperty(ModelDescriptions.OPERATION_STATUS)
    private String status;

    @ApiModelProperty(ModelDescriptions.OPERATION_STATUS_REASON)
    private String reason;

    @ApiModelProperty(ModelDescriptions.BACKUP_INCLUDED_DATA)
    private List<String> includedData;

    @ApiModelProperty(ModelDescriptions.BACKUP_TIMESTAMP)
    private String timestamp;

    public SdxBackupStatusResponse(String operationId, String status, String reason, List<String> includedData, String timestamp) {
        this.operationId = operationId;
        this.status = status;
        this.reason = reason;
        this.includedData = includedData;
        this.timestamp = timestamp;
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

    @Override
    public String toString() {
        return "SdxBackupStatusResponse{" +
                "OperationId=" + operationId +
                "Status=" + status +
                "Reason=" + reason +
                '}';
    }
}
