package com.sequenceiq.datalake.entity.operation;

public enum SdxOperationType {
    NONE("NONE"),
    BACKUP("BACKUP"),
    RESTORE("RESTORE");

    private String name;

    SdxOperationType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}