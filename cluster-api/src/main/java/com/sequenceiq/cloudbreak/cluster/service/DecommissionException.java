package com.sequenceiq.cloudbreak.cluster.service;

public class DecommissionException extends RuntimeException {
    public DecommissionException(Throwable cause) {
        super(cause);
    }

    public DecommissionException(String message) {
        super(message);
    }
}
