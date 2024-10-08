package com.sequenceiq.flow.api.model.operation;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OperationStatusResponse {

    private String operationId;

    private String operationName;

    private OperationProgressStatus operationStatus;

    private OffsetDateTime started;

    private OffsetDateTime ended;

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public OperationProgressStatus getOperationStatus() {
        return operationStatus;
    }

    public void setOperationStatus(OperationProgressStatus operationStatus) {
        this.operationStatus = operationStatus;
    }

    public OffsetDateTime getStarted() {
        return started;
    }

    public void setStarted(OffsetDateTime started) {
        this.started = started;
    }

    public OffsetDateTime getEnded() {
        return ended;
    }

    public void setEnded(OffsetDateTime ended) {
        this.ended = ended;
    }
}
