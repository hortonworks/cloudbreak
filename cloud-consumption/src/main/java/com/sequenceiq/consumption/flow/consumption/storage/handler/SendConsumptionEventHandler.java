package com.sequenceiq.consumption.flow.consumption.storage.handler;

import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerSelectors.SEND_CONSUMPTION_EVENT_HANDLER;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors.STORAGE_CONSUMPTION_COLLECTION_FINISH_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionEvent;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerEvent;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class SendConsumptionEventHandler  extends AbstractStorageOperationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendConsumptionEventHandler.class);

    @Override
    public Selectable executeOperation(HandlerEvent<StorageConsumptionCollectionHandlerEvent> event) throws Exception {
        StorageConsumptionCollectionHandlerEvent data = event.getData();
        Long resourceId = data.getResourceId();
        String resourceCrn = data.getResourceCrn();
        LOGGER.debug("Sending storage consumption event started. resourceCrn: '{}'", resourceCrn);
        return StorageConsumptionCollectionEvent.builder()
                .withResourceCrn(resourceCrn)
                .withResourceId(resourceId)
                .withSelector(STORAGE_CONSUMPTION_COLLECTION_FINISH_EVENT.selector())
                .build();
    }

    @Override
    public String getOperationName() {
        return "Send storage consumption event";
    }

    @Override
    public String selector() {
        return SEND_CONSUMPTION_EVENT_HANDLER.selector();
    }
}
