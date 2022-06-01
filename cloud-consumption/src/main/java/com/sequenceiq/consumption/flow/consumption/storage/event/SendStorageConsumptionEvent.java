package com.sequenceiq.consumption.flow.consumption.storage.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.consumption.dto.StorageConsumptionResult;

import reactor.rx.Promise;

public class SendStorageConsumptionEvent extends StorageConsumptionCollectionEvent {

    private final StorageConsumptionResult storageConsumptionResult;

    public SendStorageConsumptionEvent(String selector, Long resourceId, String resourceCrn, StorageConsumptionResult storageConsumptionResult) {
        super(selector, resourceId, resourceCrn);
        this.storageConsumptionResult = storageConsumptionResult;
    }

    public SendStorageConsumptionEvent(String selector, Long resourceId, String resourceCrn, StorageConsumptionResult storageConsumptionResult,
            Promise<AcceptResult> accepted) {
        super(selector, resourceId, resourceCrn, accepted);
        this.storageConsumptionResult = storageConsumptionResult;
    }

    public StorageConsumptionResult getStorageConsumptionResult() {
        return storageConsumptionResult;
    }

    @Override
    public String toString() {
        return "SendStorageConsumptionEvent{" +
                "storageConsumptionResult=" + storageConsumptionResult +
                "} " + super.toString();
    }
}
