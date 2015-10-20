package com.sequenceiq.cloudbreak.cloud.exception;

public class CloudOperationNotSupportedException extends RuntimeException {

    public CloudOperationNotSupportedException(String message) {
        super(message);
    }

}