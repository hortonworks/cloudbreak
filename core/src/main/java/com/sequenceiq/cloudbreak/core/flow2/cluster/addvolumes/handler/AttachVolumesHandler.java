package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AttachVolumesFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AttachVolumesHandlerEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class AttachVolumesHandler extends ExceptionCatcherEventHandler<AttachVolumesHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttachVolumesHandler.class);

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AttachVolumesHandlerEvent.class);
    }

    @Override
    public Selectable doAccept(HandlerEvent<AttachVolumesHandlerEvent> attachVolumesHandlerEvent) {
        LOGGER.debug("Starting to add additional volumes on DiskUpdateService");
        AttachVolumesHandlerEvent payload = attachVolumesHandlerEvent.getData();
        Long stackId = payload.getResourceId();
        return new AttachVolumesFinishedEvent(stackId, payload.getNumberOfDisks(), payload.getType(), payload.getSize(),
                payload.getCloudVolumeUsageType(), payload.getInstanceGroup());
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<AttachVolumesHandlerEvent> event) {
        return new AddVolumesFailedEvent(resourceId, e);
    }
}