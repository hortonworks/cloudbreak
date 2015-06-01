package com.sequenceiq.cloudbreak.domain;

public enum GcpDiskMode {

    READ_WRITE("READ_WRITE");

    private final String value;

    private GcpDiskMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
