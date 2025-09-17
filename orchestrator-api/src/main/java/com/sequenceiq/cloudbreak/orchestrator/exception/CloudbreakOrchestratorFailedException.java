package com.sequenceiq.cloudbreak.orchestrator.exception;

import com.google.common.collect.Multimap;

public class CloudbreakOrchestratorFailedException extends CloudbreakOrchestratorException {
    public CloudbreakOrchestratorFailedException(String message) {
        super(message);
    }

    public CloudbreakOrchestratorFailedException(String message, Multimap<String, String> nodesWithErrors) {
        super(message, nodesWithErrors);
    }

    public CloudbreakOrchestratorFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudbreakOrchestratorFailedException(String message, Throwable cause, Multimap<String, String> nodesWithErrors) {
        super(message, cause, nodesWithErrors);
    }

    public CloudbreakOrchestratorFailedException(Throwable cause) {
        super(cause);
    }

    protected CloudbreakOrchestratorFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
