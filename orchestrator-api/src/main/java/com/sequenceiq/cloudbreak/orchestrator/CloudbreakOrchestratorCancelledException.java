package com.sequenceiq.cloudbreak.orchestrator;

public class CloudbreakOrchestratorCancelledException extends CloudbreakOrchestratorException {

    public CloudbreakOrchestratorCancelledException(String message) {
        super(message);
    }

    public CloudbreakOrchestratorCancelledException(String message, Throwable cause) {
        super(message, cause);
    }

    protected CloudbreakOrchestratorCancelledException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public CloudbreakOrchestratorCancelledException(Throwable cause) {
        super(cause);
    }
}
