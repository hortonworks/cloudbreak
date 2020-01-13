package com.sequenceiq.cloudbreak.orchestrator.exception;

import com.google.common.collect.Multimap;

public class CloudbreakOrchestratorTimeoutException extends CloudbreakOrchestratorException {

    private final Long timeoutMinutes;

    public CloudbreakOrchestratorTimeoutException(String message, Long timeoutMinutes) {
        super(message);
        this.timeoutMinutes = timeoutMinutes;
    }

    public CloudbreakOrchestratorTimeoutException(String message, Multimap<String, String> nodesWithErrors, Long timeoutMinute) {
        super(message, nodesWithErrors);
        this.timeoutMinutes = timeoutMinute;
    }

    public CloudbreakOrchestratorTimeoutException(String message, Throwable cause, Long timeoutMinute) {
        super(message, cause);
        this.timeoutMinutes = timeoutMinute;
    }

    public CloudbreakOrchestratorTimeoutException(Throwable cause, Long timeoutMinute) {
        super(cause);
        this.timeoutMinutes = timeoutMinute;
    }

    public CloudbreakOrchestratorTimeoutException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Long timeoutMinute) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.timeoutMinutes = timeoutMinute;
    }

    public Long getTimeoutMinutes() {
        return timeoutMinutes;
    }
}
