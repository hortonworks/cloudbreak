package com.sequenceiq.environment.environment.flow.modify.tags.event;

import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.FAILED_MODIFY_USER_DEFINED_TAGS_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

public class EnvTagsModificationFailureEvent extends BaseFailedFlowEvent {

    private final EnvironmentStatus environmentStatus;

    @JsonCreator
    public EnvTagsModificationFailureEvent(
            @JsonProperty("resourceId") Long environmentId,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("environmentStatus") EnvironmentStatus environmentStatus,
            @JsonProperty("exception") Exception exception) {

        super(FAILED_MODIFY_USER_DEFINED_TAGS_EVENT.selector(), environmentId, resourceName, resourceCrn, exception);
        this.environmentStatus = environmentStatus;
    }

    @Override
    public String selector() {
        return FAILED_MODIFY_USER_DEFINED_TAGS_EVENT.event();
    }

    public EnvironmentStatus getEnvironmentStatus() {
        return environmentStatus;
    }

    @Override
    public String toString() {
        return "EnvTagsModificationFailureEvent{" +
                "environmentStatus='" + environmentStatus +
                '}';
    }
}