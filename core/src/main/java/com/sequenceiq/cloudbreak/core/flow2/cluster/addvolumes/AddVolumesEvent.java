package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes;

import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.request.AddVolumesCMConfigFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.request.AddVolumesCMConfigHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.request.AddVolumesFailedEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum AddVolumesEvent implements FlowEvent {
    ADD_VOLUMES_CM_CONFIGURATION_HANDLER_EVENT(EventSelectorUtil.selector(AddVolumesCMConfigHandlerEvent.class)),
    ADD_VOLUMES_CM_CONFIGURATION_FINISHED_EVENT(EventSelectorUtil.selector(AddVolumesCMConfigFinishedEvent.class)),
    FAILURE_EVENT(EventSelectorUtil.selector(AddVolumesFailedEvent.class));

    private final String event;

    AddVolumesEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}