package com.sequenceiq.consumption.flow.consumption.storage.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.consumption.flow.consumption.ConsumptionContext;

import reactor.rx.Promise;

public class StorageConsumptionCollectionHandlerEvent extends StorageConsumptionCollectionEvent {

    private final ConsumptionContext context;

    public StorageConsumptionCollectionHandlerEvent(String selector, Long resourceId, String resourceCrn, ConsumptionContext context) {
        super(selector, resourceId, resourceCrn);
        this.context = context;
    }

    public StorageConsumptionCollectionHandlerEvent(String selector, Long resourceId, String resourceCrn, ConsumptionContext context,
            Promise<AcceptResult> accepted) {
        super(selector, resourceId, resourceCrn, accepted);
        this.context = context;
    }

    public ConsumptionContext getContext() {
        return context;
    }

    @Override
    public String toString() {
        return "StorageConsumptionCollectionHandlerEvent{" +
                "context=" + context.toString() +
                "} " + super.toString();
    }
}
