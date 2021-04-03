package com.sequenceiq.periscope.service;

public class AutoScalingException extends RuntimeException {
    public AutoScalingException(String message) {
        super(message);
    }

    public AutoScalingException(String message, Throwable cause) {
        super(message, cause);
    }
}
