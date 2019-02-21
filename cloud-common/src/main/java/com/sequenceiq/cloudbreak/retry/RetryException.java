package com.sequenceiq.cloudbreak.retry;

public class RetryException extends RuntimeException {

    public RetryException() {
    }

    public RetryException(String message) {
        super(message);
    }
}
