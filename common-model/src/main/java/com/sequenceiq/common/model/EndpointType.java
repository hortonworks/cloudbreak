package com.sequenceiq.common.model;

public enum EndpointType {
    NONE,
    USE_SERVICE_ENDPOINT,
    USE_PRIVATE_ENDPOINT;

    public static EndpointType safeValueOf(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
