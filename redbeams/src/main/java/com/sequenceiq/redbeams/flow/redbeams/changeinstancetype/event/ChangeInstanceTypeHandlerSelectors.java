package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum ChangeInstanceTypeHandlerSelectors implements FlowEvent {

    SYNC_DATABASE_SERVER_INSTANCE_TYPE_HANDLER,
    VALIDATE_CHANGE_INSTANCE_TYPE_HANDLER,
    CHANGE_DATABASE_SERVER_INSTANCE_TYPE_HANDLER,
    WAIT_FOR_CHANGE_INSTANCE_TYPE_HANDLER;

    @Override
    public String event() {
        return name();
    }
}
