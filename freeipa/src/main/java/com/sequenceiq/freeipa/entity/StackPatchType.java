package com.sequenceiq.freeipa.entity;

public enum StackPatchType {
    MINIFI_RETRY_CONFIG_FIX,
    UNKNOWN;

    private final StackPatchTypeStatus status;

    StackPatchType() {
        status = StackPatchTypeStatus.ACTIVE;
    }

    StackPatchType(StackPatchTypeStatus status) {
        this.status = status;
    }

    public StackPatchTypeStatus getStatus() {
        return status;
    }
}
