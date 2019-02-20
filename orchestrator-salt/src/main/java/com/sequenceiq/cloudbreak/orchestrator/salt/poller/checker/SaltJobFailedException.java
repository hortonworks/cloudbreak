package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

public class SaltJobFailedException extends Exception {

    public SaltJobFailedException() {
    }

    public SaltJobFailedException(String message) {
        super(message);
    }

    public SaltJobFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}
