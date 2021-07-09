package com.sequenceiq.flow.api.model.operation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.flow.api.model.FlowProgressResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OperationView implements Serializable {

    private static final int DEFAULT_PROGRESS = -1;

    private String operationId;

    private OperationType operationType = OperationType.UNKNOWN;

    private OperationResource operationResource = OperationResource.UNKNOWN;

    private List<FlowProgressResponse> operations = new ArrayList<>();

    private Map<OperationResource, OperationView> subOperations = new HashMap<>();

    private Map<OperationResource, OperationCondition> subOperationConditions = new HashMap<>();

    private OperationProgressStatus progressStatus = OperationProgressStatus.UNKNOWN;

    private Integer progress = DEFAULT_PROGRESS;

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public List<FlowProgressResponse> getOperations() {
        return operations;
    }

    public void setOperations(List<FlowProgressResponse> operations) {
        this.operations = operations;
    }

    public OperationResource getOperationResource() {
        return operationResource;
    }

    public void setOperationResource(OperationResource operationResource) {
        this.operationResource = operationResource;
    }

    public Map<OperationResource, OperationView> getSubOperations() {
        return subOperations;
    }

    public void setSubOperations(Map<OperationResource, OperationView> subOperations) {
        this.subOperations = subOperations;
    }

    public Map<OperationResource, OperationCondition> getSubOperationConditions() {
        return subOperationConditions;
    }

    public void setSubOperationConditions(Map<OperationResource, OperationCondition> subOperationConditions) {
        this.subOperationConditions = subOperationConditions;
    }

    public OperationProgressStatus getProgressStatus() {
        return progressStatus;
    }

    public void setProgressStatus(OperationProgressStatus progressStatus) {
        this.progressStatus = progressStatus;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    @Override
    public String toString() {
        return "OperationView{" +
                "operationId='" + operationId + '\'' +
                ", operationType=" + operationType +
                ", operationResource=" + operationResource +
                ", operations=" + operations +
                ", subOperations=" + subOperations +
                ", subOperationConditions=" + subOperationConditions +
                ", progressStatus=" + progressStatus +
                ", progress=" + progress +
                '}';
    }
}
