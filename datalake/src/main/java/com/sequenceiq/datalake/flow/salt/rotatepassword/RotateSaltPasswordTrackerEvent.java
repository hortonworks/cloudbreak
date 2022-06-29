package com.sequenceiq.datalake.flow.salt.rotatepassword;

import com.sequenceiq.datalake.flow.salt.rotatepassword.event.RotateSaltPasswordFailureResponse;
import com.sequenceiq.datalake.flow.salt.rotatepassword.event.RotateSaltPasswordSuccessResponse;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum RotateSaltPasswordTrackerEvent implements FlowEvent {

    ROTATE_SALT_PASSWORD_EVENT,
    ROTATE_SALT_PASSWORD_FAILED_EVENT(EventSelectorUtil.selector(RotateSaltPasswordFailureResponse.class)),
    ROTATE_SALT_PASSWORD_FAIL_HANDLED_EVENT,
    ROTATE_SALT_PASSWORD_SUCCESS_EVENT(EventSelectorUtil.selector(RotateSaltPasswordSuccessResponse.class)),
    ROTATE_SALT_PASSWORD_FINISHED_EVENT;

    private final String event;

    RotateSaltPasswordTrackerEvent() {
        this.event = name();
    }

    RotateSaltPasswordTrackerEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
