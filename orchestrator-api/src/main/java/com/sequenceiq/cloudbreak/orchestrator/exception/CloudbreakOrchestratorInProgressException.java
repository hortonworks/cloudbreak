package com.sequenceiq.cloudbreak.orchestrator.exception;

public class CloudbreakOrchestratorInProgressException extends CloudbreakOrchestratorException {

    public CloudbreakOrchestratorInProgressException(String message) {
        super(message);
    }

    public CloudbreakOrchestratorInProgressException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudbreakOrchestratorInProgressException(Throwable cause) {
        super(cause);
    }

    protected CloudbreakOrchestratorInProgressException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
