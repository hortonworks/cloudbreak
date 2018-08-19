package com.sequenceiq.cloudbreak.structuredevent.event;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class StructuredEvent implements Serializable {

    public static final String TYPE_FIELD = "type";

    private String type;

    private OperationDetails operation;

    private Long orgId;

    private String userId;

    public StructuredEvent() {
    }

    public StructuredEvent(String type, OperationDetails operation, Long orgId, String userId) {
        this.type = type;
        this.operation = operation;
        this.orgId = orgId;
        this.userId = userId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setOperation(OperationDetails operation) {
        this.operation = operation;
    }

    public OperationDetails getOperation() {
        return operation;
    }

    public abstract String getStatus();

    public abstract Long getDuration();

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
