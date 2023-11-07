package com.sequenceiq.datalake.flow.delete.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.flow.delete.event.SdxDeletionFailedEvent;
import com.sequenceiq.datalake.flow.delete.event.StorageConsumptionCollectionUnschedulingRequest;
import com.sequenceiq.datalake.flow.delete.event.StorageConsumptionCollectionUnschedulingSuccessEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class StorageConsumptionCollectionUnschedulingHandler extends ExceptionCatcherEventHandler<StorageConsumptionCollectionUnschedulingRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageConsumptionCollectionUnschedulingHandler.class);

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(StorageConsumptionCollectionUnschedulingRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<StorageConsumptionCollectionUnschedulingRequest> event) {
        return new SdxDeletionFailedEvent(resourceId, null, e, event.getData().isForced());
    }

    @Override
    protected Selectable doAccept(HandlerEvent<StorageConsumptionCollectionUnschedulingRequest> event) {
        LOGGER.debug("Storage consumption collection unscheduling flow step started.");
        StorageConsumptionCollectionUnschedulingRequest request = event.getData();
        Long sdxId = request.getResourceId();
        Selectable response = new StorageConsumptionCollectionUnschedulingSuccessEvent(sdxId, request.getUserId(), request.isForced());
        LOGGER.debug("Storage consumption collection unscheduling flow step finished.");
        return response;
    }

}
