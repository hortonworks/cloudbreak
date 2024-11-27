package com.sequenceiq.common.model;

public interface DatabaseType {
    String name();

    String shortName();

    default boolean isDatabasePauseSupported() {
        return true;
    }

    String referenceType();
}
