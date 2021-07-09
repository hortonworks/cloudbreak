package com.sequenceiq.flow.api.model.operation;

public enum OperationCondition {
    NONE, REQUIRED;

    public static OperationCondition fromBoolean(boolean value) {
        return value ? REQUIRED : NONE;
    }
}
