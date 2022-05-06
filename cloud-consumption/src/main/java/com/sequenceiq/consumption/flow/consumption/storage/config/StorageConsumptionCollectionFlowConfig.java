package com.sequenceiq.consumption.flow.consumption.storage.config;

import static com.sequenceiq.consumption.flow.consumption.storage.StorageConsumptionCollectionState.FINAL_STATE;
import static com.sequenceiq.consumption.flow.consumption.storage.StorageConsumptionCollectionState.INIT_STATE;
import static com.sequenceiq.consumption.flow.consumption.storage.StorageConsumptionCollectionState.SEND_CONSUMPTION_EVENT_STATE;
import static com.sequenceiq.consumption.flow.consumption.storage.StorageConsumptionCollectionState.STORAGE_CONSUMPTION_COLLECTION_FAILED_STATE;
import static com.sequenceiq.consumption.flow.consumption.storage.StorageConsumptionCollectionState.STORAGE_CONSUMPTION_COLLECTION_FINISHED_STATE;
import static com.sequenceiq.consumption.flow.consumption.storage.StorageConsumptionCollectionState.STORAGE_CONSUMPTION_COLLECTION_STATE;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors.SEND_CONSUMPTION_EVENT_EVENT;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors.STORAGE_CONSUMPTION_COLLECTION_FAILED_EVENT;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors.STORAGE_CONSUMPTION_COLLECTION_FINALIZED_EVENT;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors.STORAGE_CONSUMPTION_COLLECTION_FINISH_EVENT;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors.STORAGE_CONSUMPTION_COLLECTION_HANDLED_FAILED_EVENT;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors.STORAGE_CONSUMPTION_COLLECTION_START_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.consumption.flow.consumption.storage.StorageConsumptionCollectionState;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class StorageConsumptionCollectionFlowConfig extends AbstractFlowConfiguration<StorageConsumptionCollectionState,
        StorageConsumptionCollectionStateSelectors> {

    private static final List<Transition<StorageConsumptionCollectionState, StorageConsumptionCollectionStateSelectors>> TRANSITIONS
            = new Transition.Builder<StorageConsumptionCollectionState, StorageConsumptionCollectionStateSelectors>()
            .defaultFailureEvent(STORAGE_CONSUMPTION_COLLECTION_FAILED_EVENT)

            .from(INIT_STATE).to(STORAGE_CONSUMPTION_COLLECTION_STATE)
            .event(STORAGE_CONSUMPTION_COLLECTION_START_EVENT)
            .defaultFailureEvent()

            .from(STORAGE_CONSUMPTION_COLLECTION_STATE).to(SEND_CONSUMPTION_EVENT_STATE)
            .event(SEND_CONSUMPTION_EVENT_EVENT)
            .defaultFailureEvent()

            .from(SEND_CONSUMPTION_EVENT_STATE).to(STORAGE_CONSUMPTION_COLLECTION_FINISHED_STATE)
            .event(STORAGE_CONSUMPTION_COLLECTION_FINISH_EVENT)
            .defaultFailureEvent()

            .from(STORAGE_CONSUMPTION_COLLECTION_FINISHED_STATE).to(FINAL_STATE)
            .event(STORAGE_CONSUMPTION_COLLECTION_FINALIZED_EVENT)
            .defaultFailureEvent()

            .build();

    private static final FlowEdgeConfig<StorageConsumptionCollectionState, StorageConsumptionCollectionStateSelectors> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, STORAGE_CONSUMPTION_COLLECTION_FAILED_STATE, STORAGE_CONSUMPTION_COLLECTION_HANDLED_FAILED_EVENT);

    protected StorageConsumptionCollectionFlowConfig() {
        super(StorageConsumptionCollectionState.class, StorageConsumptionCollectionStateSelectors.class);
    }

    @Override
    protected List<Transition<StorageConsumptionCollectionState, StorageConsumptionCollectionStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<StorageConsumptionCollectionState, StorageConsumptionCollectionStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public StorageConsumptionCollectionStateSelectors[] getEvents() {
        return StorageConsumptionCollectionStateSelectors.values();
    }

    @Override
    public StorageConsumptionCollectionStateSelectors[] getInitEvents() {
        return new StorageConsumptionCollectionStateSelectors[] {
                STORAGE_CONSUMPTION_COLLECTION_START_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Storage consumption collection flow";
    }
}
