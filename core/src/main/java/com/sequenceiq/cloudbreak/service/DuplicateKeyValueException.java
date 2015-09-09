package com.sequenceiq.cloudbreak.service;


import com.sequenceiq.cloudbreak.domain.APIResourceType;

public class DuplicateKeyValueException extends RuntimeException {
    private final APIResourceType resourceType;
    private final String value;

    public DuplicateKeyValueException(APIResourceType resourceType, String value) {
        this.resourceType = resourceType;
        this.value = value;
    }

    public DuplicateKeyValueException(APIResourceType resourceType, String value, Throwable cause) {
        super(cause);
        this.resourceType = resourceType;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public APIResourceType getResourceType() {
        return resourceType;
    }
}
