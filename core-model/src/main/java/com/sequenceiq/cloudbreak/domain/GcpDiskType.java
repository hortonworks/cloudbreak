package com.sequenceiq.cloudbreak.domain;

public enum GcpDiskType {

    PERSISTENT("PERSISTENT");

    private final String value;

    private GcpDiskType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
