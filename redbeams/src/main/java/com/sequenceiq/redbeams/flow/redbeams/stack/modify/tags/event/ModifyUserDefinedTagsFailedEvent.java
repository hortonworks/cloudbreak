package com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event;

import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.FAILED_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;

public class ModifyUserDefinedTagsFailedEvent extends RedbeamsFailureEvent {

    @JsonCreator
    public ModifyUserDefinedTagsFailedEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception) {
        super(FAILED_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT.name(), resourceId, exception, false);
    }

    @Override
    public String toString() {
        return "ModifyUserDefinedTagsFailedEvent{"
                + "resourceId=" + getResourceId()
                + ", exception=" + getException()
                + '}';
    }
}
