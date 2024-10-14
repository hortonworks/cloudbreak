package com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.event;

import static com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.FreeIpaProviderTemplateUpdateFlowEvent.FREEIPA_PROVIDER_TEMPLATE_UPDATE_FAILURE_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class FreeIpaProviderTemplateUpdateFailureEvent extends StackFailureEvent {

    private final String failedPhase;

    @JsonCreator
    public FreeIpaProviderTemplateUpdateFailureEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("failedPhase") String failedPhase,
            @JsonProperty("exception") Exception exception) {
        super(FREEIPA_PROVIDER_TEMPLATE_UPDATE_FAILURE_EVENT.selector(), stackId, exception);
        this.failedPhase = failedPhase;
    }

    public String getFailedPhase() {
        return failedPhase;
    }
}
