package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesFinishedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesHandlerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesRequest;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum DeleteVolumesEvent implements FlowEvent {

    DELETE_VOLUMES_VALIDATION_EVENT("DELETE_VOLUMES_TRIGGER_EVENT"),
    DELETE_VOLUMES_VALIDATION_HANDLER_EVENT(EventSelectorUtil.selector(DeleteVolumesValidationRequest.class)),
    DELETE_VOLUMES_EVENT(EventSelectorUtil.selector(DeleteVolumesRequest.class)),
    DELETE_VOLUMES_HANDLER_EVENT(EventSelectorUtil.selector(DeleteVolumesHandlerRequest.class)),
    DELETE_VOLUMES_FINISHED_EVENT(EventSelectorUtil.selector(DeleteVolumesFinishedEvent.class)),
    DELETE_VOLUMES_UNMOUNT_HANDLER_EVENT(EventSelectorUtil.selector(DeleteVolumesUnmountEvent.class)),
    DELETE_VOLUMES_UNMOUNT_FINISHED_EVENT(EventSelectorUtil.selector(DeleteVolumesUnmountFinishedEvent.class)),
    DELETE_VOLUMES_CM_CONFIG_HANDLER_EVENT(EventSelectorUtil.selector(DeleteVolumesCMConfigEvent.class)),
    DELETE_VOLUMES_CM_CONFIG_FINISHED_EVENT(EventSelectorUtil.selector(DeleteVolumesCMConfigFinishedEvent.class)),
    FINALIZED_EVENT(EventSelectorUtil.selector(DeleteVolumesFinalizedEvent.class)),
    FAIL_HANDLED_EVENT(CloudPlatformResult.failureSelector(DeleteVolumesFailedEvent.class));
    private final String event;

    DeleteVolumesEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
