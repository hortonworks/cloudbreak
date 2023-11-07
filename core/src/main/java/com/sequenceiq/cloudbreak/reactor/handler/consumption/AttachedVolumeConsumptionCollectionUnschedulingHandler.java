package com.sequenceiq.cloudbreak.reactor.handler.consumption;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption.AttachedVolumeConsumptionCollectionUnschedulingFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption.AttachedVolumeConsumptionCollectionUnschedulingRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption.AttachedVolumeConsumptionCollectionUnschedulingSuccess;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class AttachedVolumeConsumptionCollectionUnschedulingHandler extends
        ExceptionCatcherEventHandler<AttachedVolumeConsumptionCollectionUnschedulingRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttachedVolumeConsumptionCollectionUnschedulingHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AttachedVolumeConsumptionCollectionUnschedulingRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<AttachedVolumeConsumptionCollectionUnschedulingRequest> event) {
        return new AttachedVolumeConsumptionCollectionUnschedulingFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<AttachedVolumeConsumptionCollectionUnschedulingRequest> event) {
        LOGGER.debug("Attached volume consumption collection unscheduling flow step started.");
        AttachedVolumeConsumptionCollectionUnschedulingRequest request = event.getData();

        Long resourceId = request.getResourceId();

        LOGGER.debug("Attached volume consumption collection unscheduling flow step finished.");
        return new AttachedVolumeConsumptionCollectionUnschedulingSuccess(resourceId);
    }

}
