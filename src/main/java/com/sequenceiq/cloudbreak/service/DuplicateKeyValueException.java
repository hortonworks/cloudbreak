package com.sequenceiq.cloudbreak.service;


public class DuplicateKeyValueException extends RuntimeException {
    private final String value;

    public DuplicateKeyValueException(String value, Throwable cause) {
        super(cause);
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
