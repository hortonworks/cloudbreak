package com.sequenceiq.environment.environment.flow.modify.proxy.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvProxyModificationStateSelectors implements FlowEvent {

    MODIFY_PROXY_START_EVENT,
    MODIFY_PROXY_FREEIPA_EVENT,
    MODIFY_PROXY_DATALAKE_EVENT,
    FINISH_MODIFY_PROXY_EVENT,
    FINALIZE_MODIFY_PROXY_EVENT,
    HANDLE_MODIFY_PROXY_EVENT,
    HANDLE_FAILED_MODIFY_PROXY_EVENT,
    FAILED_MODIFY_PROXY_EVENT;

    @Override
    public String event() {
        return name();
    }
}
