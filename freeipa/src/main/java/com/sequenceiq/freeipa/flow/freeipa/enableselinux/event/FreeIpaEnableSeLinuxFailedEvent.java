package com.sequenceiq.freeipa.flow.freeipa.enableselinux.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class FreeIpaEnableSeLinuxFailedEvent extends StackFailureEvent {

    private final String failedPhase;

    @JsonCreator
    public FreeIpaEnableSeLinuxFailedEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("failedPhase") String failedPhase,
            @JsonProperty("exception") Exception exception) {

        super(FreeIpaEnableSeLinuxStateSelectors.FAILED_ENABLE_SELINUX_FREEIPA_EVENT.name(), stackId, exception);
        this.failedPhase = failedPhase;
    }

    public String getFailedPhase() {
        return failedPhase;
    }
}
