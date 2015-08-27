package com.sequenceiq.cloudbreak.cloud.model;


public class CloudOperationNotSupportedException extends RuntimeException {

    public CloudOperationNotSupportedException(String message) {
        super(message);
    }
}
