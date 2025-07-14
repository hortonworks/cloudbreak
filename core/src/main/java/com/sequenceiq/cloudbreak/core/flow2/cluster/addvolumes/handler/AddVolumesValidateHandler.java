package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesValidateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.service.AddVolumesService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class AddVolumesValidateHandler extends ExceptionCatcherEventHandler<AddVolumesValidateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddVolumesValidateHandler.class);

    @Inject
    private AddVolumesService addVolumesService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AddVolumesValidateEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<AddVolumesValidateEvent> event) {
        LOGGER.error("Unexpected error happened during volume addition validation.", e);
        return new AddVolumesFailedEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<AddVolumesValidateEvent> event) {
        AddVolumesValidateEvent payload = event.getData();
        Long stackId = payload.getResourceId();
        String instanceGroupName = payload.getInstanceGroup();
        try {
            addVolumesService.validateVolumeAddition(stackId, instanceGroupName);
            return new AddVolumesValidationFinishedEvent(stackId, payload.getNumberOfDisks(), payload.getType(),
                    payload.getSize(), payload.getCloudVolumeUsageType(), instanceGroupName);
        } catch (Exception e) {
            LOGGER.warn("Add disks validation failed.", e);
            return new AddVolumesFailedEvent(stackId, e);
        }
    }
}
