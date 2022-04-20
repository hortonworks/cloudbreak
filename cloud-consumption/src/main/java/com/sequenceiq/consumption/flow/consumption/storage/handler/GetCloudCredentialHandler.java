package com.sequenceiq.consumption.flow.consumption.storage.handler;

import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerSelectors.GET_CLOUD_CREDENTIAL_HANDLER;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors.STORAGE_CONSUMPTION_COLLECTION_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionEvent;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class GetCloudCredentialHandler extends AbstractStorageOperationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetCloudCredentialHandler.class);

    @Override
    public Selectable executeOperation(HandlerEvent<StorageConsumptionCollectionEvent> event) throws Exception {
        StorageConsumptionCollectionEvent data = event.getData();
        Long resourceId = data.getResourceId();
        String resourceCrn = data.getResourceCrn();
        LOGGER.debug("Getting cloud credential started. resourceCrn: '{}'", resourceCrn);
        return StorageConsumptionCollectionEvent.builder()
                .withResourceCrn(resourceCrn)
                .withResourceId(resourceId)
                .withEnvironmentCrn(data.getEnvironmentCrn())
                .withStorageLocation(data.getStorageLocation())
                .withSelector(STORAGE_CONSUMPTION_COLLECTION_EVENT.selector())
                .build();
    }

    @Override
    public String getOperationName() {
        return "Get cloud credential";
    }

    @Override
    public String selector() {
        return GET_CLOUD_CREDENTIAL_HANDLER.selector();
    }
}
