package com.sequenceiq.datalake.entity.operation;

public enum SdxOperationStatus {
    INIT("INIT"),
    TRIGGERRED("TRIGGERRED"),
    INPROGRESS("INPROGRESS"),
    SUCCEEDED("SUCCEEDED"),
    FAILED("FAILED");

    private String name;

    SdxOperationStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
