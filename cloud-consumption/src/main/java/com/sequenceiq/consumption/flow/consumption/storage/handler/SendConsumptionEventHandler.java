package com.sequenceiq.consumption.flow.consumption.storage.handler;

import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerSelectors.SEND_CONSUMPTION_EVENT_HANDLER;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors.STORAGE_CONSUMPTION_COLLECTION_FINISH_EVENT;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.metering.events.MeteringEventsProto;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.cloudbreak.usage.MeteringEventProcessor;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.dto.StorageConsumptionResult;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionEvent;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerEvent;
import com.sequenceiq.consumption.converter.metering.ConsumptionToStorageHeartbeatConverter;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class SendConsumptionEventHandler  extends AbstractStorageOperationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendConsumptionEventHandler.class);

    @Inject
    private MeteringEventProcessor meteringEventProcessor;

    @Inject
    private ConsumptionToStorageHeartbeatConverter storageHeartbeatConverter;

    @Override
    public Selectable executeOperation(HandlerEvent<StorageConsumptionCollectionHandlerEvent> event) throws Exception {
        StorageConsumptionCollectionHandlerEvent data = event.getData();
        Long resourceId = data.getResourceId();
        String resourceCrn = data.getResourceCrn();
        LOGGER.debug("Sending storage consumption event started. resourceCrn: '{}'", resourceCrn);

        Consumption consumption = data.getContext().getConsumption();
        StorageConsumptionResult storage = data.getStorageConsumptionResult();

        if (storage != null) {
            MeteringEventsProto.StorageHeartbeat heartbeat = storageHeartbeatConverter.convertToS3StorageHeartBeat(consumption, storage);
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
