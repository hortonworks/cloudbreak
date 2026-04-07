package com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ModifyUserDefinedTagsFailedEvent extends StackFailureEvent {
    private final String failedPhase;

    public ModifyUserDefinedTagsFailedEvent(Long stackId, String failedPhase, Exception exception) {
        this(ModifyUserDefinedTagsStateSelectors.FAILED_MODIFY_USER_DEFINED_TAGS_EVENT.name(), stackId, failedPhase, exception);
    }

    @JsonCreator
    public ModifyUserDefinedTagsFailedEvent(
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
