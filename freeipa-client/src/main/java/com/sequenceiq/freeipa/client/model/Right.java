package com.sequenceiq.freeipa.client.model;

import java.util.Locale;

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
        value = name().toLowerCase(Locale.ROOT);
    }

    public String getValue() {
        return value;
    }
}
