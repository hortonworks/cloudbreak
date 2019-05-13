package com.sequenceiq.cloudbreak.cloud.scheduler;

public class CancellationException extends RuntimeException {
    public CancellationException(String message) {
        super(message);
    }
}
