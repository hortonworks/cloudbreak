package com.sequenceiq.freeipa.flow.stack.modify.proxy.selector;

import com.sequenceiq.flow.core.FlowEvent;

public enum ModifyProxyConfigEvent implements FlowEvent {

    MODIFY_PROXY_TRIGGER_EVENT,
    MODIFY_PROXY_SUCCESS_EVENT,
    MODIFY_PROXY_FAILED_EVENT,
    MODIFY_PROXY_FAILURE_HANDLED_EVENT,
    MODIFY_PROXY_FINISHED_EVENT;

    @Override
    public String event() {
        return name();
    }
}
