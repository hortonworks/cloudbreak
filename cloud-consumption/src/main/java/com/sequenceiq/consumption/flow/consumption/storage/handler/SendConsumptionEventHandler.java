package com.sequenceiq.consumption.flow.consumption.storage.handler;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerSelectors.SEND_CONSUMPTION_EVENT_HANDLER;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors.STORAGE_CONSUMPTION_COLLECTION_FINISH_EVENT;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.metering.events.MeteringEventsProto;
import com.sequenceiq.cloudbreak.cloud.ConsumptionCalculator;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudConsumption;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.cloudbreak.usage.MeteringEventProcessor;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.dto.StorageConsumptionResult;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionEvent;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerEvent;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class SendConsumptionEventHandler  extends AbstractStorageOperationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendConsumptionEventHandler.class);

    @Inject
    private MeteringEventProcessor meteringEventProcessor;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Selectable executeOperation(HandlerEvent<StorageConsumptionCollectionHandlerEvent> event) throws Exception {
        StorageConsumptionCollectionHandlerEvent consumptionEvent = event.getData();
        Long resourceId = consumptionEvent.getResourceId();
        String resourceCrn = consumptionEvent.getResourceCrn();
        LOGGER.debug("Sending storage consumption event started. resourceCrn: '{}'", resourceCrn);
        Consumption consumption = consumptionEvent.getContext().getConsumption();
        CloudConsumption cloudConsumption = CloudConsumption.builder()
                .withStorageLocation(consumption.getStorageLocation())
                .withEnvironmentCrn(consumption.getEnvironmentCrn())
                .build();
        LOGGER.debug("Getting credential for environment with CRN '{}'.", consumption.getEnvironmentCrn());
        String cloudPlatform = consumption.getConsumptionType().getStorageService().cloudPlatformName();
        StorageConsumptionResult storage = consumptionEvent.getStorageConsumptionResult();
        Optional<ConsumptionCalculator> consumptionCalculator = cloudPlatformConnectors
                .getDefault(platform(cloudPlatform))
                .consumptionCalculator(consumption.getConsumptionType().getStorageService());
        if (storage != null && consumptionCalculator.isPresent()) {
            MeteringEventsProto.StorageHeartbeat heartbeat = consumptionCalculator.get()
                    .convertToStorageHeartbeat(cloudConsumption, storage.getStorageInBytes());
            meteringEventProcessor.storageHeartbeat(heartbeat, MeteringEventsProto.ServiceType.Value.ENVIRONMENT);
            LOGGER.debug("StorageHeartbeat event was successfully sent for Consumption with CRN [{}]", resourceCrn);

            return StorageConsumptionCollectionEvent.builder()
                    .withResourceCrn(resourceCrn)
                    .withResourceId(resourceId)
                    .withSelector(STORAGE_CONSUMPTION_COLLECTION_FINISH_EVENT.selector())
                    .build();
        } else {
            String message = String.format("StorageConsumptionResult missing from StorageConsumptionCollectionHandlerEvent, " +
                    "cannot send StorageHeartbeat for Consumption with CRN [%s].", resourceCrn);
            LOGGER.error(message);
            throw new OperationException(message);
        }
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
