package com.sequenceiq.consumption.flow.consumption.storage.handler;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.consumption.flow.consumption.storage.StorageConsumptionCollectionFlowException;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionEvent;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionFailureEvent;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

public abstract class AbstractStorageOperationHandler extends ExceptionCatcherEventHandler<StorageConsumptionCollectionEvent> {

    @Override
    protected Selectable doAccept(HandlerEvent<StorageConsumptionCollectionEvent> event) {
        try {
            return executeOperation(event);
        } catch (Exception e) {
            throw new StorageConsumptionCollectionFlowException(
                    String.format("Error during storage consumption collection operation: %s%n%s", getOperationName(), e.getMessage()), e);
        }
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<StorageConsumptionCollectionEvent> event) {
        return new StorageConsumptionCollectionFailureEvent(resourceId, e, event.getData().getResourceCrn(), event.getData().getEnvironmentCrn(),
                event.getData().getStorageLocation());
    }

    public abstract Selectable executeOperation(HandlerEvent<StorageConsumptionCollectionEvent> data) throws Exception;

    public abstract String getOperationName();
}
