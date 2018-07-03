package com.sequenceiq.cloudbreak.orchestrator.exception;

import com.google.common.collect.Multimap;

public class CloudbreakOrchestratorTerminateException extends CloudbreakOrchestratorException {

    public CloudbreakOrchestratorTerminateException(String message) {
        super(message);
    }

    public CloudbreakOrchestratorTerminateException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudbreakOrchestratorTerminateException(Throwable cause) {
        super(cause);
    }

    protected CloudbreakOrchestratorTerminateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public CloudbreakOrchestratorTerminateException(String message, Multimap<String, String> nodesWithError) {
        super(message, nodesWithError);
    }
}
