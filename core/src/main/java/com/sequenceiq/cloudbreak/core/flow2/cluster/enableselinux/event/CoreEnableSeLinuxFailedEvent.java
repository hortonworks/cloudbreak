package com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class CoreEnableSeLinuxFailedEvent extends StackFailureEvent {

    private final String failedPhase;

    public CoreEnableSeLinuxFailedEvent(Long stackId, String failedPhase, Exception exception) {
        this(CoreEnableSeLinuxStateSelectors.FAILED_ENABLE_SELINUX_CORE_EVENT.name(), stackId, failedPhase, exception);
    }

    @JsonCreator
    public CoreEnableSeLinuxFailedEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("failedPhase") String failedPhase,
            @JsonProperty("exception") Exception exception) {
        super(selector, stackId, exception);
        this.failedPhase = failedPhase;
    }

    public String getFailedPhase() {
        return failedPhase;
    }
}
