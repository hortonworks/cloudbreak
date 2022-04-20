package com.sequenceiq.consumption.flow.consumption.storage.handler;

import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerSelectors.STORAGE_CONSUMPTION_COLLECTION_HANDLER;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors.SEND_CONSUMPTION_EVENT_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionEvent;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class StorageConsumptionCollectionHandler  extends AbstractStorageOperationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageConsumptionCollectionHandler.class);

    @Override
    public Selectable executeOperation(HandlerEvent<StorageConsumptionCollectionEvent> event) throws Exception {
        StorageConsumptionCollectionEvent data = event.getData();
        Long resourceId = data.getResourceId();
        String resourceCrn = data.getResourceCrn();
        LOGGER.debug("Storage consumption collection started. resourceCrn: '{}'", resourceCrn);
        return StorageConsumptionCollectionEvent.builder()
                .withResourceCrn(resourceCrn)
                .withResourceId(resourceId)
                .withEnvironmentCrn(data.getEnvironmentCrn())
                .withStorageLocation(data.getStorageLocation())
                .withSelector(SEND_CONSUMPTION_EVENT_EVENT.selector())
                .build();
    }

    @Override
    public String getOperationName() {
        return "Collect storage consumption data";
    }

    @Override
    public String selector() {
        return STORAGE_CONSUMPTION_COLLECTION_HANDLER.selector();
    }
}
