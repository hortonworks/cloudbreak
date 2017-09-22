package com.sequenceiq.cloudbreak.structuredevent.event;

public class StructuredEvent {
    public static final String TYPE_FIELD = "type";

    private String type;

    private OperationDetails operation;

    protected StructuredEvent(String type, OperationDetails operation) {
        this.type = type;
        this.operation = operation;
    }

    public String getType() {
        return type;
    }

    public OperationDetails getOperation() {
        return operation;
    }
}
