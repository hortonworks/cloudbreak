package com.sequenceiq.cloudbreak.core.flow2.cluster.salt.rotatepassword;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordFailureResponse;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordSuccessResponse;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

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
