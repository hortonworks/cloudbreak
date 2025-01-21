package com.sequenceiq.cloudbreak.reactor.handler.consumption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption.AttachedVolumeConsumptionCollectionSchedulingFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption.AttachedVolumeConsumptionCollectionSchedulingRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption.AttachedVolumeConsumptionCollectionSchedulingSuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class AttachedVolumeConsumptionCollectionSchedulingHandler extends ExceptionCatcherEventHandler<AttachedVolumeConsumptionCollectionSchedulingRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttachedVolumeConsumptionCollectionSchedulingHandler.class);

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AttachedVolumeConsumptionCollectionSchedulingRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<AttachedVolumeConsumptionCollectionSchedulingRequest> event) {
        return new AttachedVolumeConsumptionCollectionSchedulingFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<AttachedVolumeConsumptionCollectionSchedulingRequest> event) {
        LOGGER.debug("Attached volume consumption collection scheduling flow step started.");
        AttachedVolumeConsumptionCollectionSchedulingRequest request = event.getData();

        Long resourceId = request.getResourceId();

        LOGGER.debug("Attached volume consumption collection scheduling flow step finished.");
        return new AttachedVolumeConsumptionCollectionSchedulingSuccess(resourceId);
    }

}
