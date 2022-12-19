package com.sequenceiq.freeipa.client.model;

public enum Right {
    WRITE,
    READ,
    SEARCH,
    COMPARE,
    ADD,
    DELETE,
    ALL;

    private String value;

    Right() {
        value = name().toLowerCase();
    }

    public String getValue() {
        return value;
    }
}
