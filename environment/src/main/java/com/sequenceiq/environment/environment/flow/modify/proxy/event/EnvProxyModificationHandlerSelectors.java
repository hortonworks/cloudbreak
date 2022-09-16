package com.sequenceiq.environment.environment.flow.modify.proxy.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvProxyModificationHandlerSelectors implements FlowEvent {
    SAVE_NEW_PROXY_ASSOCIATION_HANDLER_EVENT;

    @Override
    public String event() {
        return name();
    }
}
