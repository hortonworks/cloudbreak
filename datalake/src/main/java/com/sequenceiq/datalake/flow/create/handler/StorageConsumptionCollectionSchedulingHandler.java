package com.sequenceiq.datalake.flow.create.handler;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.flow.create.event.StorageConsumptionCollectionSchedulingRequest;
import com.sequenceiq.datalake.flow.create.event.StorageConsumptionCollectionSchedulingSuccessEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.consumption.ConsumptionService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class StorageConsumptionCollectionSchedulingHandler extends ExceptionCatcherEventHandler<StorageConsumptionCollectionSchedulingRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageConsumptionCollectionSchedulingHandler.class);

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private ConsumptionService consumptionService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(StorageConsumptionCollectionSchedulingRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<StorageConsumptionCollectionSchedulingRequest> event) {
        return new SdxCreateFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<StorageConsumptionCollectionSchedulingRequest> event) {
        LOGGER.debug("Storage consumption collection scheduling flow step started.");
        StorageConsumptionCollectionSchedulingRequest request = event.getData();

        Long datalakeId = request.getResourceId();
        sdxClusterRepository.findById(datalakeId)
                .ifPresentOrElse(consumptionService::scheduleStorageConsumptionCollectionIfNeeded,
                        () -> {
                            throw notFound("SDX cluster", datalakeId).get();
                        });

        Selectable response = new StorageConsumptionCollectionSchedulingSuccessEvent(datalakeId, request.getUserId(), request.getDetailedEnvironmentResponse());
        LOGGER.debug("Storage consumption collection scheduling flow step finished.");
        return response;
    }

}
