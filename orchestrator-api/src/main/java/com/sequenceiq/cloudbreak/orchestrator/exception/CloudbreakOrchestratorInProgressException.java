package com.sequenceiq.cloudbreak.orchestrator.exception;

import com.google.common.collect.Multimap;

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

    public CloudbreakOrchestratorInProgressException(String message, Multimap<String, String> nodesWithError) {
        super(message, nodesWithError);
    }
}
