package com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain;

public enum GccDiskType {

    PERSISTENT("PERSISTENT");

    private final String value;

    private GccDiskType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
