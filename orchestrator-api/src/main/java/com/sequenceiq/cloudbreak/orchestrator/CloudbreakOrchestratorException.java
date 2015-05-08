package com.sequenceiq.cloudbreak.orchestrator;

public class CloudbreakOrchestratorException extends Exception {

    public CloudbreakOrchestratorException(String message) {
        super(message);
    }

    public CloudbreakOrchestratorException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudbreakOrchestratorException(Throwable cause) {
        super(cause);
    }

    protected CloudbreakOrchestratorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
