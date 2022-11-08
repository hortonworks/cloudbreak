package com.sequenceiq.datalake.flow.salt.update;

import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateFailureResponse;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateSuccessResponse;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateTriggerEvent;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateWaitSuccessResponse;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum SaltUpdateEvent implements FlowEvent {

    SALT_UPDATE_EVENT(EventSelectorUtil.selector(SaltUpdateTriggerEvent.class)),
    SALT_UPDATE_FAILED_EVENT(EventSelectorUtil.selector(SaltUpdateFailureResponse.class)),
    SALT_UPDATE_FAIL_HANDLED_EVENT,
    SALT_UPDATE_SUCCESS_EVENT(EventSelectorUtil.selector(SaltUpdateSuccessResponse.class)),
    SALT_UPDATE_WAIT_SUCCESS_EVENT(EventSelectorUtil.selector(SaltUpdateWaitSuccessResponse.class)),
    SALT_UPDATE_FINISHED_EVENT;

    private final String event;

    SaltUpdateEvent() {
        this.event = name();
    }

    SaltUpdateEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
