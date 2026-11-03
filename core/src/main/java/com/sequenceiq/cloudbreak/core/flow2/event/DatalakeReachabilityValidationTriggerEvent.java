package com.sequenceiq.cloudbreak.core.flow2.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DatalakeReachabilityValidationTriggerEvent extends StackEvent {

    private final DetailedStackStatus failureStatus;

    @JsonCreator
    public DatalakeReachabilityValidationTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("failureStatus") DetailedStackStatus failureStatus) {
        super(selector, stackId);
        this.failureStatus = failureStatus;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return other instanceof DatalakeReachabilityValidationTriggerEvent
                && isClassAndEqualsEvent(DatalakeReachabilityValidationTriggerEvent.class, other,
                event -> failureStatus == event.failureStatus);
    }

    public DetailedStackStatus getFailureStatus() {
        return failureStatus;
    }

    @Override
    public String toString() {
        return "DatalakeReachabilityValidationTriggerEvent{"
                + "failureStatus=" + failureStatus
                + "} " + super.toString();
    }
}
