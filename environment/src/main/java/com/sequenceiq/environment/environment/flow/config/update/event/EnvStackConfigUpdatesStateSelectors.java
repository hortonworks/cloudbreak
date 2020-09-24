package com.sequenceiq.environment.environment.flow.config.update.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvStackConfigUpdatesStateSelectors implements FlowEvent {
    ENV_STACK_CONFIG_UPDATES_START_EVENT,
    FINISH_ENV_STACK_CONFIG_UPDATES_EVENT,
    FINALIZE_ENV_STACK_CONIFG_UPDATES_EVENT,
    HANDLE_FAILED_ENV_STACK_CONIFG_UPDATES_EVENT,
    FAILED_ENV_STACK_CONIFG_UPDATES_EVENT;

    @Override
    public String event() {
        return name();
    }
}
