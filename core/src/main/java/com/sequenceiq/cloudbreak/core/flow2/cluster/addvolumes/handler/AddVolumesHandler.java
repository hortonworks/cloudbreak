package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesHandlerEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class AddVolumesHandler extends ExceptionCatcherEventHandler<AddVolumesHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddVolumesHandler.class);

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AddVolumesHandlerEvent.class);
    }

    @Override
    public Selectable doAccept(HandlerEvent<AddVolumesHandlerEvent> addVolumesHandlerEvent) {
        AddVolumesHandlerEvent payload = addVolumesHandlerEvent.getData();
        Long stackId = payload.getResourceId();
        String instanceGroup = payload.getInstanceGroup();
        LOGGER.info("Starting AddVolumesHandler - adding volumes to group {}", instanceGroup);
        return new AddVolumesFinishedEvent(stackId, payload.getNumberOfDisks(), payload.getType(), payload.getSize(),
                payload.getCloudVolumeUsageType(), payload.getInstanceGroup());
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<AddVolumesHandlerEvent> event) {
        return new AddVolumesFailedEvent(resourceId, e);
    }
}