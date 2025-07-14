package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes;

import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesCMConfigFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesCMConfigHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesFinalizedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesOrchestrationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesOrchestrationHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesValidateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AttachVolumesFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AttachVolumesHandlerEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum AddVolumesEvent implements FlowEvent {
    ADD_VOLUMES_TRIGGER_EVENT("ADD_VOLUMES_TRIGGER_EVENT"),
    ADD_VOLUMES_VALIDATE_HANDLER_EVENT(EventSelectorUtil.selector(AddVolumesValidateEvent.class)),
    ADD_VOLUMES_VALIDATION_FINISHED_EVENT(EventSelectorUtil.selector(AddVolumesValidationFinishedEvent.class)),
    ADD_VOLUMES_HANDLER_EVENT(EventSelectorUtil.selector(AddVolumesHandlerEvent.class)),
    ADD_VOLUMES_FINISHED_EVENT(EventSelectorUtil.selector(AddVolumesFinishedEvent.class)),
    ATTACH_VOLUMES_HANDLER_EVENT(EventSelectorUtil.selector(AttachVolumesHandlerEvent.class)),
    ATTACH_VOLUMES_FINISHED_EVENT(EventSelectorUtil.selector(AttachVolumesFinishedEvent.class)),
    ADD_VOLUMES_ORCHESTRATION_HANDLER_EVENT(EventSelectorUtil.selector(AddVolumesOrchestrationHandlerEvent.class)),
    ADD_VOLUMES_CM_CONFIGURATION_HANDLER_EVENT(EventSelectorUtil.selector(AddVolumesCMConfigHandlerEvent.class)),
    ADD_VOLUMES_ORCHESTRATION_FINISHED_EVENT(EventSelectorUtil.selector(AddVolumesOrchestrationFinishedEvent.class)),
    ADD_VOLUMES_CM_CONFIGURATION_FINISHED_EVENT(EventSelectorUtil.selector(AddVolumesCMConfigFinishedEvent.class)),
    FINALIZED_EVENT(EventSelectorUtil.selector(AddVolumesFinalizedEvent.class)),
    FAILURE_EVENT(EventSelectorUtil.selector(AddVolumesFailedEvent.class)),
    ADD_VOLUMES_FAILURE_HANDLED_EVENT("ADD_VOLUMES_FAILURE_HANDLED_EVENT");

    private final String event;

    AddVolumesEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}