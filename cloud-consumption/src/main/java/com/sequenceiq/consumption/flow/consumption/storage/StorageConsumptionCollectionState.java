package com.sequenceiq.consumption.flow.consumption.storage;

import com.sequenceiq.consumption.flow.ConsumptionFillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum StorageConsumptionCollectionState implements FlowState {
    INIT_STATE,
    GET_CLOUD_CREDENTIAL_STATE,
    STORAGE_CONSUMPTION_COLLECTION_STATE,
    SEND_CONSUMPTION_EVENT_STATE,
    STORAGE_CONSUMPTION_COLLECTION_FINISHED_STATE,
    STORAGE_CONSUMPTION_COLLECTION_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return ConsumptionFillInMemoryStateStoreRestartAction.class;
    }
}
