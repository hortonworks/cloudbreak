package com.sequenceiq.freeipa.flow.freeipa.upscale.event;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class UpscaleFailureEvent extends StackEvent {

    private final Exception exception;

    private final String failedPhase;

    private final Set<String> success;

    private final Map<String, String> failureDetails;

    public UpscaleFailureEvent(Long stackId, String failedPhase, Set<String> success, Map<String, String> failureDetails,
            Exception exception) {
        super(stackId);
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

    @Override
    public String toString() {
        return "UpscaleFailureEvent{" +
                "exception=" + exception +
                ", failedPhase='" + failedPhase + '\'' +
                ", success=" + success +
                ", failureDetails=" + failureDetails +
                "} " + super.toString();
    }
}
