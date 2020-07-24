package com.sequenceiq.environment.environment.flow.config.update.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvStackConfigUpdatesHandlerSelectors implements FlowEvent {
    STACK_CONFIG_UPDATES_HANDLER_EVENT;

    @Override
    public String event() {
        return name();
    }
}