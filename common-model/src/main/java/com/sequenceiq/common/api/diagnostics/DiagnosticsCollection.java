package com.sequenceiq.common.api.diagnostics;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiagnosticsCollection implements Serializable {

    private DiagnosticsCollectionStatus status;

    private String flowId;

    private String currentFlowStatus;

    private Long created;

    private Integer progressPercentage;

    private Map<String, Object> properties;

    public DiagnosticsCollectionStatus getStatus() {
        return status;
    }

    public void setStatus(DiagnosticsCollectionStatus status) {
        this.status = status;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getCurrentFlowStatus() {
        return currentFlowStatus;
    }

    public void setCurrentFlowStatus(String currentFlowStatus) {
        this.currentFlowStatus = currentFlowStatus;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Integer getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Integer progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
}
