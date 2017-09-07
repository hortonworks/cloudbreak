package com.sequenceiq.cloudbreak.orchestrator.exception;

public abstract class CloudbreakOrchestratorException extends Exception {

    protected CloudbreakOrchestratorException(String message) {
        super(message);
    }

    protected CloudbreakOrchestratorException(String message, Throwable cause) {
        super(message, cause);
    }

    protected CloudbreakOrchestratorException(Throwable cause) {
        super(cause);
    }

    protected CloudbreakOrchestratorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
