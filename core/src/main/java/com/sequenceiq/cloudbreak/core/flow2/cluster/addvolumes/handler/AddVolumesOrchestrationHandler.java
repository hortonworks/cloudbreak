package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesOrchestrationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesOrchestrationHandlerEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class AddVolumesOrchestrationHandler extends ExceptionCatcherEventHandler<AddVolumesOrchestrationHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddVolumesOrchestrationHandler.class);

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AddVolumesOrchestrationHandlerEvent.class);
    }

    @Override
    public Selectable doAccept(HandlerEvent<AddVolumesOrchestrationHandlerEvent> addVolumesOrchestrationHandlerEvent) {
        AddVolumesOrchestrationHandlerEvent payload = addVolumesOrchestrationHandlerEvent.getData();
        Long stackId = payload.getResourceId();
        String requestGroup = payload.getInstanceGroup();
        LOGGER.info("Starting orchestration after adding volumes to group {}", requestGroup);
        return new AddVolumesOrchestrationFinishedEvent(stackId, payload.getNumberOfDisks(), payload.getType(), payload.getSize(),
                payload.getCloudVolumeUsageType(), requestGroup);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<AddVolumesOrchestrationHandlerEvent> event) {
        return new AddVolumesFailedEvent(resourceId, e);
    }
}
