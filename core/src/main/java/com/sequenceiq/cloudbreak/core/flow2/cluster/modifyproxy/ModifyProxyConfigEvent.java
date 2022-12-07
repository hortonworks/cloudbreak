package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigFailureResponse;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigSuccessResponse;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum ModifyProxyConfigEvent implements FlowEvent {

    MODIFY_PROXY_CONFIG_EVENT,
    MODIFY_PROXY_CONFIG_ON_CM,
    MODIFY_PROXY_CONFIG_FAILED_EVENT(EventSelectorUtil.selector(ModifyProxyConfigFailureResponse.class)),
    MODIFY_PROXY_CONFIG_FAIL_HANDLED_EVENT,
    MODIFY_PROXY_CONFIG_SUCCESS_EVENT(EventSelectorUtil.selector(ModifyProxyConfigSuccessResponse.class)),
    MODIFY_PROXY_CONFIG_FINISHED_EVENT;

    private final String event;

    ModifyProxyConfigEvent() {
        this.event = name();
    }

    ModifyProxyConfigEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
