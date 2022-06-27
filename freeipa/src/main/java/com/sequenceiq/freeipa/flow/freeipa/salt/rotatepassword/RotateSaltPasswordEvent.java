package com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event.RotateSaltPasswordFailureResponse;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event.RotateSaltPasswordSuccessResponse;

public enum RotateSaltPasswordEvent implements FlowEvent {

    ROTATE_SALT_PASSWORD_EVENT("ROTATE_SALT_PASSWORD_EVENT"),
    ROTATE_SALT_PASSWORD_FAILED_EVENT(EventSelectorUtil.selector(RotateSaltPasswordFailureResponse.class)),
    ROTATE_SALT_PASSWORD_FAIL_HANDLED_EVENT("ROTATE_SALT_PASSWORD_FAIL_HANDLED"),
    ROTATE_SALT_PASSWORD_SUCCESS_EVENT(EventSelectorUtil.selector(RotateSaltPasswordSuccessResponse.class)),
    ROTATE_SALT_PASSWORD_FINISHED_EVENT("ROTATE_SALT_PASSWORD_FINISHED_EVENT");

    private final String event;

    RotateSaltPasswordEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
