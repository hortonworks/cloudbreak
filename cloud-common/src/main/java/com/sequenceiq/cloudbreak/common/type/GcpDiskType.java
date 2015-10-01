package com.sequenceiq.cloudbreak.common.type;

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
