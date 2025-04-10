package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class CoreModifySeLinuxFailedEvent extends StackFailureEvent {

    private final String failedPhase;

    public CoreModifySeLinuxFailedEvent(Long stackId, String failedPhase, Exception exception) {
        this(CoreModifySeLinuxStateSelectors.FAILED_MODIFY_SELINUX_CORE_EVENT.name(), stackId, failedPhase, exception);
    }

    @JsonCreator
    public CoreModifySeLinuxFailedEvent(
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
