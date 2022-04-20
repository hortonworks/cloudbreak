package com.sequenceiq.consumption.flow.consumption.storage;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionEvent;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerSelectors;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors;
import com.sequenceiq.flow.core.CommonContext;

@Configuration
public class StorageConsumptionCollectionActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageConsumptionCollectionActions.class);

    @Bean(name = "GET_CLOUD_CREDENTIAL_STATE")
    public Action<?, ?> getCloudCredentialAction() {
        return new AbstractStorageConsumptionCollectionAction<>(StorageConsumptionCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, StorageConsumptionCollectionEvent payload, Map<Object, Object> variables) {
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into GET_CLOUD_CREDENTIAL_STATE. resourceCrn: '{}'", resourceCrn);
                StorageConsumptionCollectionEvent event = StorageConsumptionCollectionEvent.builder()
                        .withResourceId(payload.getResourceId())
                        .withResourceCrn(resourceCrn)
                        .withEnvironmentCrn(payload.getEnvironmentCrn())
                        .withStorageLocation(payload.getStorageLocation())
                        .withSelector(StorageConsumptionCollectionHandlerSelectors.GET_CLOUD_CREDENTIAL_HANDLER.selector())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "STORAGE_CONSUMPTION_COLLECTION_STATE")
    public Action<?, ?> storageConsumptionCollectionAction() {
        return new AbstractStorageConsumptionCollectionAction<>(StorageConsumptionCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, StorageConsumptionCollectionEvent payload, Map<Object, Object> variables) {
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into STORAGE_CONSUMPTION_COLLECTION_STATE. resourceCrn: '{}'", resourceCrn);
                StorageConsumptionCollectionEvent event = StorageConsumptionCollectionEvent.builder()
                        .withResourceId(payload.getResourceId())
                        .withResourceCrn(resourceCrn)
                        .withEnvironmentCrn(payload.getEnvironmentCrn())
                        .withStorageLocation(payload.getStorageLocation())
                        .withSelector(StorageConsumptionCollectionHandlerSelectors.STORAGE_CONSUMPTION_COLLECTION_HANDLER.selector())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "SEND_CONSUMPTION_EVENT_STATE")
    public Action<?, ?> sendConsumptionEventAction() {
        return new AbstractStorageConsumptionCollectionAction<>(StorageConsumptionCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, StorageConsumptionCollectionEvent payload, Map<Object, Object> variables) {
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into SEND_CONSUMPTION_EVENT_STATE. resourceCrn: '{}'", resourceCrn);
                StorageConsumptionCollectionEvent event = StorageConsumptionCollectionEvent.builder()
                        .withResourceId(payload.getResourceId())
                        .withResourceCrn(resourceCrn)
                        .withEnvironmentCrn(payload.getEnvironmentCrn())
                        .withStorageLocation(payload.getStorageLocation())
                        .withSelector(StorageConsumptionCollectionHandlerSelectors.SEND_CONSUMPTION_EVENT_HANDLER.selector())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "STORAGE_CONSUMPTION_COLLECTION_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractStorageConsumptionCollectionAction<>(StorageConsumptionCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, StorageConsumptionCollectionEvent payload, Map<Object, Object> variables) {
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into STORAGE_CONSUMPTION_COLLECTION_FINISHED_STATE. resourceCrn: '{}'", resourceCrn);
                StorageConsumptionCollectionEvent event = StorageConsumptionCollectionEvent.builder()
                        .withResourceId(payload.getResourceId())
                        .withResourceCrn(resourceCrn)
                        .withEnvironmentCrn(payload.getEnvironmentCrn())
                        .withStorageLocation(payload.getStorageLocation())
                        .withSelector(StorageConsumptionCollectionStateSelectors.STORAGE_CONSUMPTION_COLLECTION_FINALIZED_EVENT.selector())
                        .build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "STORAGE_CONSUMPTION_COLLECTION_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractStorageConsumptionCollectionAction<>(StorageConsumptionCollectionEvent.class) {
            @Override
            protected void doExecute(CommonContext context, StorageConsumptionCollectionEvent payload, Map<Object, Object> variables) {
                String resourceCrn = payload.getResourceCrn();
                LOGGER.debug("Flow entered into STORAGE_CONSUMPTION_COLLECTION_FAILED_STATE. resourceCrn: '{}'", resourceCrn);
                StorageConsumptionCollectionEvent event = StorageConsumptionCollectionEvent.builder()
                        .withResourceId(payload.getResourceId())
                        .withResourceCrn(resourceCrn)
                        .withEnvironmentCrn(payload.getEnvironmentCrn())
                        .withStorageLocation(payload.getStorageLocation())
                        .withSelector(StorageConsumptionCollectionStateSelectors.STORAGE_CONSUMPTION_COLLECTION_HANDLED_FAILED_EVENT.selector())
                        .build();
                sendEvent(context, event);
            }
        };
    }
}
