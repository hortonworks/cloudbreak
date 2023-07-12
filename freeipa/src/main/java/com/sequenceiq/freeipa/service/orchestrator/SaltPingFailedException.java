package com.sequenceiq.freeipa.service.orchestrator;

public class SaltPingFailedException extends Exception {

    public SaltPingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
