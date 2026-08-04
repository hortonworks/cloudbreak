package com.sequenceiq.cloudbreak.sdx.common.model;

public class DistroXOperationValidationView {

    private DistroXOperations operation;

    private boolean allowed;

    private String reason;

    public DistroXOperations getOperation() {
        return operation;
    }

    public void setOperation(DistroXOperations operation) {
        this.operation = operation;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
