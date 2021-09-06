package com.sequenceiq.cloudbreak.structuredevent.event.cdp;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.cloudbreak.structuredevent.event.CDPStructuredEventDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = CDPStructuredEventDeserializer.class)
public abstract class CDPStructuredEvent implements Serializable {

    public static final String TYPE_FIELD = "type";

    public static final String SENT = "SENT";

    public static final long ZERO = 0L;

    private String type;

    private CDPOperationDetails operation;

    private String status;

    private String statusReason;

    public CDPStructuredEvent() {
    }

    public CDPStructuredEvent(String type) {
        this.type = type;
    }

    public CDPStructuredEvent(String type, CDPOperationDetails operation, String status, String statusReason) {
        this.type = type;
        this.operation = operation;
        this.status = status;
        this.statusReason = statusReason;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setOperation(CDPOperationDetails operation) {
        this.operation = operation;
    }

    public CDPOperationDetails getOperation() {
        return operation;
    }

    public abstract String getStatus();

    public abstract Long getDuration();

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "type='" + type + '\'' +
                ", operation=" + operation +
                ", status='" + status + '\'' +
                ", statusReason='" + statusReason + '\'' +
                '}';
    }
}
