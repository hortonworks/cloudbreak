package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event;

import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_VALIDATION_FAILED_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;

/**
 * Signals that the pre-flight validation rejected the change instance type request. It carries its own selector so the failure action can
 * tell a rejected request apart from a failed modification and set the status accordingly, without inspecting the exception type.
 */
public class ChangeInstanceTypeValidationFailedEvent extends RedbeamsFailureEvent {

    @JsonCreator
    public ChangeInstanceTypeValidationFailedEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception) {
        super(CHANGE_INSTANCE_TYPE_VALIDATION_FAILED_EVENT.event(), resourceId, exception, false);
    }

    @Override
    public String toString() {
        return "ChangeInstanceTypeValidationFailedEvent{"
                + "resourceId=" + getResourceId()
                + ", exception=" + getException()
                + '}';
    }
}
