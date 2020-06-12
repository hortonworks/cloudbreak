package com.sequenceiq.freeipa.flow.freeipa.downscale.event;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class DownscaleFailureEvent extends StackEvent {

    private final Exception exception;

    private final String failedPhase;

    private final Set<String> success;

    private final Map<String, String> failureDetails;

    public DownscaleFailureEvent(Long stackId, String failedPhase, Set<String> success, Map<String, String> failureDetails,
            Exception exception) {
        super(null, stackId);
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
