package com.sequenceiq.cloudbreak.cloud.retry;

public class RetryException extends RuntimeException {

    public RetryException() {
    }

    public RetryException(String message) {
        super(message);
    }
}
