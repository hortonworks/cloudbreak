package com.sequenceiq.datalake.flow.modifyproxy;

import com.sequenceiq.datalake.flow.modifyproxy.event.ModifyProxyConfigFailureResponse;
import com.sequenceiq.datalake.flow.modifyproxy.event.ModifyProxyConfigSuccessResponse;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum ModifyProxyConfigTrackerEvent implements FlowEvent {

    MODIFY_PROXY_CONFIG_EVENT,
    MODIFY_PROXY_CONFIG_FAILED_EVENT(EventSelectorUtil.selector(ModifyProxyConfigFailureResponse.class)),
    MODIFY_PROXY_CONFIG_FAIL_HANDLED_EVENT,
    MODIFY_PROXY_CONFIG_SUCCESS_EVENT(EventSelectorUtil.selector(ModifyProxyConfigSuccessResponse.class)),
    MODIFY_PROXY_CONFIG_FINISHED_EVENT;

    private final String event;

    ModifyProxyConfigTrackerEvent() {
        this.event = name();
    }

    ModifyProxyConfigTrackerEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
