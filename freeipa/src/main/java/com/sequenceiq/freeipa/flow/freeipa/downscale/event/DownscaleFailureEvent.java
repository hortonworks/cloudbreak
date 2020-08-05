package com.sequenceiq.freeipa.flow.freeipa.downscale.event;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

import java.util.Map;
import java.util.Set;

public class DownscaleFailureEvent extends StackEvent {

    private final Exception exception;

    private final String failedPhase;

    private final Set<String> success;

    private final Map<String, String> failureDetails;

    public DownscaleFailureEvent(Long stackId, String failedPhase, Set<String> success, Map<String, String> failureDetails,
            Exception exception) {
        this(null, stackId, failedPhase, success, failureDetails, exception);
    }

    public DownscaleFailureEvent(String selector, Long stackId, String failedPhase, Set<String> success, Map<String, String> failureDetails,
            Exception exception) {
        super(selector, stackId);
        this.exception = exception;
        this.failedPhase = failedPhase;
        this.success = success;
        this.failureDetails = failureDetails;
    }

    public Exception getException() {
        return exception;
    }

    public String getFailedPhase() {
        return failedPhase;
    }

    public Set<String> getSuccess() {
        return success;
    }

    public Map<String, String> getFailureDetails() {
        return failureDetails;
    }

}
