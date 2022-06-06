package com.sequenceiq.consumption.flow.consumption.storage.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.consumption.dto.StorageConsumptionResult;
import com.sequenceiq.consumption.flow.consumption.ConsumptionContext;

import reactor.rx.Promise;

public class StorageConsumptionCollectionHandlerEvent extends StorageConsumptionCollectionEvent {

    private final ConsumptionContext context;

    private final StorageConsumptionResult storageConsumptionResult;

    public StorageConsumptionCollectionHandlerEvent(String selector, Long resourceId, String resourceCrn, ConsumptionContext context,
            StorageConsumptionResult storageConsumptionResult) {
        super(selector, resourceId, resourceCrn);
        this.context = context;
        this.storageConsumptionResult = storageConsumptionResult;
    }

    public StorageConsumptionCollectionHandlerEvent(String selector, Long resourceId, String resourceCrn, ConsumptionContext context,
            StorageConsumptionResult storageConsumptionResult, Promise<AcceptResult> accepted) {
        super(selector, resourceId, resourceCrn, accepted);
        this.context = context;
        this.storageConsumptionResult = storageConsumptionResult;
    }

    public ConsumptionContext getContext() {
        return context;
    }

    public StorageConsumptionResult getStorageConsumptionResult() {
        return storageConsumptionResult;
    }

    @Override
    public String toString() {
        return "StorageConsumptionCollectionHandlerEvent{" +
                "context=" + context +
                ", storageConsumptionResult=" + storageConsumptionResult +
                "} " + super.toString();
    }
}
