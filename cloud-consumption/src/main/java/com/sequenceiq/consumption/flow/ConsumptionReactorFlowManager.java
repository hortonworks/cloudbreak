package com.sequenceiq.consumption.flow;

import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors.STORAGE_CONSUMPTION_COLLECTION_START_EVENT;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionEvent;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@Service
public class ConsumptionReactorFlowManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionReactorFlowManager.class);

    private final EventSender eventSender;

    public ConsumptionReactorFlowManager(EventSender eventSender) {
        this.eventSender = eventSender;
    }

    private Map<String, Object> getFlowTriggerUsercrn(String userCrn) {
        return Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn);
    }

    public void triggerStorageConsumptionCollectionFlow(Consumption consumption, String userCrn) {
        LOGGER.info("Storage consumption collection flow triggered for environment {} and location {}", consumption.getEnvironmentCrn(),
                consumption.getStorageLocation());
        StorageConsumptionCollectionEvent event = StorageConsumptionCollectionEvent.builder()
                .withResourceCrn(consumption.getResourceCrn())
                .withResourceId(consumption.getId())
                .withSelector(STORAGE_CONSUMPTION_COLLECTION_START_EVENT.event())
                .withAccepted(new Promise<>())
                .build();
        eventSender.sendEvent(event, new Event.Headers(getFlowTriggerUsercrn(userCrn)));
    }
}
