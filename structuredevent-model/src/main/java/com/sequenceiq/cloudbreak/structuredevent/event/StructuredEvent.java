package com.sequenceiq.cloudbreak.structuredevent.event;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = StructuredEventDeserializer.class)
public abstract class StructuredEvent implements Serializable {

    public static final String TYPE_FIELD = "type";

    private String type;

    private OperationDetails operation;

    public StructuredEvent() {
    }

    public StructuredEvent(String type) {
        this.type = type;
    }

    public StructuredEvent(String type, OperationDetails operation) {
        this.type = type;
        this.operation = operation;
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
}
