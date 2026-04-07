package com.sequenceiq.freeipa.flow.stack.modify.tags.event;

import static com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.FAILED_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class ModifyUserDefinedTagsFailedEvent extends StackFailureEvent {

    private final String failedPhase;

    @JsonCreator
    public ModifyUserDefinedTagsFailedEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("failedPhase") String failedPhase,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("failureType") FailureType failureType) {
        super(FAILED_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT.name(), stackId, exception, failureType);
        this.failedPhase = failedPhase;
    }

    public String getFailedPhase() {
        return failedPhase;
    }
}
