package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event;

import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.FAILED_CHANGE_INSTANCE_TYPE_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;

public class ChangeInstanceTypeFailedEvent extends RedbeamsFailureEvent {

    @JsonCreator
    public ChangeInstanceTypeFailedEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception) {
        super(FAILED_CHANGE_INSTANCE_TYPE_EVENT.event(), resourceId, exception, false);
    }

    @Override
    public String toString() {
        return "ChangeInstanceTypeFailedEvent{"
                + "resourceId=" + getResourceId()
                + ", exception=" + getException()
                + '}';
    }
}
