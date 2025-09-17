package com.sequenceiq.cloudbreak.orchestrator.exception;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public abstract class CloudbreakOrchestratorException extends Exception {

    private final Multimap<String, String> nodesWithErrors = ArrayListMultimap.create();

    protected CloudbreakOrchestratorException(String message) {
        super(message);
    }

    protected CloudbreakOrchestratorException(String message, Multimap<String, String> nodesWithErrors) {
        super(message);
        if (nodesWithErrors != null && !nodesWithErrors.isEmpty()) {
            this.nodesWithErrors.putAll(nodesWithErrors);
        }
    }

    protected CloudbreakOrchestratorException(String message, Throwable cause) {
        super(message, cause);
    }

    protected CloudbreakOrchestratorException(String message, Throwable cause, Multimap<String, String> nodesWithErrors) {
        super(message, cause);
        if (nodesWithErrors != null && !nodesWithErrors.isEmpty()) {
            this.nodesWithErrors.putAll(nodesWithErrors);
        }
    }

    protected CloudbreakOrchestratorException(Throwable cause) {
        super(cause);
    }

    protected CloudbreakOrchestratorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public Multimap<String, String> getNodesWithErrors() {
        return ArrayListMultimap.create(nodesWithErrors);
    }
}
