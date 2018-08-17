package com.sequenceiq.cloudbreak.structuredevent.event;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class StructuredEvent implements Serializable {

    public static final String TYPE_FIELD = "type";

    private String type;

    private OperationDetails operation;

    private long orgId;

    public StructuredEvent() {
    }

    public StructuredEvent(String type, OperationDetails operation, long orgId) {
        this.type = type;
        this.operation = operation;
        this.orgId = orgId;
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

    public void setOrgId(long orgId) {
        this.orgId = orgId;
    }

    public long getOrgId() {
        return orgId;
    }
}
