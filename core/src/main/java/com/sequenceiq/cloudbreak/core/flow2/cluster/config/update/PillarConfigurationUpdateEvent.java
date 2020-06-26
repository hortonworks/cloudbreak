package com.sequenceiq.cloudbreak.core.flow2.cluster.config.update;

import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.event.PillarConfigUpdateFailed;
import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.event.PillarConfigUpdateSuccess;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum PillarConfigurationUpdateEvent implements FlowEvent {
    PILLAR_CONFIG_UPDATE_EVENT("PILLAR_CONFIG_UPDATE_TRIGGER_EVENT"),
    PILLAR_CONFIG_UPDATE_FINISHED_EVENT(EventSelectorUtil.selector(PillarConfigUpdateSuccess.class)),
    PILLAR_CONFIG_UPDATE_FAILED_EVENT(EventSelectorUtil.selector(PillarConfigUpdateFailed.class)),
    PILLAR_CONFIG_UPDATE_FINALIZE_EVENT("PILLAR_CONFIG_UPDATE_FINALIZE"),
    PILLAR_CONFIG_UPDATE_FAILURE_HANDLED_EVENT("PILLAR_CONFIG_UPDATE_FAIL_HANDLED");

    private final String event;

    PillarConfigurationUpdateEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
