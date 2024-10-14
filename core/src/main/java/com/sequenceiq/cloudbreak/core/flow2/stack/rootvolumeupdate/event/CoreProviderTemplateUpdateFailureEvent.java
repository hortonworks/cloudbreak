package com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.event;

import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateFlowEvent.CORE_PROVIDER_TEMPLATE_UPDATE_FAILURE_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class CoreProviderTemplateUpdateFailureEvent extends StackFailureEvent {

    private final String failedPhase;

    @JsonCreator
    public CoreProviderTemplateUpdateFailureEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("failedPhase") String failedPhase,
            @JsonProperty("exception") Exception exception) {
        super(CORE_PROVIDER_TEMPLATE_UPDATE_FAILURE_EVENT.selector(), stackId, exception);
        this.failedPhase = failedPhase;
    }

    public String getFailedPhase() {
        return failedPhase;
    }
}
