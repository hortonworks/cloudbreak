package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.failure;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class CleanupFailureEvent extends AbstractCleanupEvent {

    private final String failedPhase;

    private final Map<String, String> failureDetails;

    private final Set<String> success;

    public CleanupFailureEvent(CleanupEvent cleanupEvent, String failedPhase, Map<String, String> failureDetails,
            Set<String> success) {
        super(cleanupEvent);
        this.failedPhase = failedPhase;
        this.failureDetails = failureDetails;
        this.success = success;
    }

    public String getFailedPhase() {
        return failedPhase;
    }

    public Map<String, String> getFailureDetails() {
        return failureDetails;
    }

    public Set<String> getSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return "CleanupFailureEvent{" +
                "failedPhase='" + failedPhase + '\'' +
                ", failureDetails=" + failureDetails +
                ", success=" + success +
                '}';
    }
}
