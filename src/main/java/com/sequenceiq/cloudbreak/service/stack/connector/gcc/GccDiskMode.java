package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

public enum GccDiskMode {

    READ_WRITE("READ_WRITE");

    private final String value;

    private GccDiskMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
