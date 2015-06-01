package com.sequenceiq.cloudbreak.orchestrator;

public class CloudbreakOrchestratorFailedException extends CloudbreakOrchestratorException {

    public CloudbreakOrchestratorFailedException(String message) {
        super(message);
    }

    public CloudbreakOrchestratorFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudbreakOrchestratorFailedException(Throwable cause) {
        super(cause);
    }

    protected CloudbreakOrchestratorFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
