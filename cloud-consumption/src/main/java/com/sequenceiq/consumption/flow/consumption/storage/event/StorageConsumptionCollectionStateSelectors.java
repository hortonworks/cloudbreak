package com.sequenceiq.consumption.flow.consumption.storage.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum StorageConsumptionCollectionStateSelectors implements FlowEvent {
    STORAGE_CONSUMPTION_COLLECTION_START_EVENT,
    SEND_CONSUMPTION_EVENT_EVENT,
    STORAGE_CONSUMPTION_COLLECTION_FINISH_EVENT,
    STORAGE_CONSUMPTION_COLLECTION_FINALIZED_EVENT,
    STORAGE_CONSUMPTION_COLLECTION_HANDLED_FAILED_EVENT,
    STORAGE_CONSUMPTION_COLLECTION_FAILED_EVENT;

    @Override
    public String event() {
        return name();
    }
}
