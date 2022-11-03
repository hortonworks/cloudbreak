package com.sequenceiq.consumption.flow.consumption.storage.handler;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.consumption.flow.consumption.storage.StorageConsumptionCollectionFlowException;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionFailureEvent;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerEvent;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

public abstract class AbstractStorageOperationHandler extends ExceptionCatcherEventHandler<StorageConsumptionCollectionHandlerEvent> {

    @Override
    protected Selectable doAccept(HandlerEvent<StorageConsumptionCollectionHandlerEvent> event) {
        try {
            return executeOperation(event);
        } catch (Exception e) {
            throw new StorageConsumptionCollectionFlowException(
                    String.format("Error during storage consumption collection operation: %s%n%s", getOperationName(), e.getMessage()), e);
        }
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<StorageConsumptionCollectionHandlerEvent> event) {
        return new StorageConsumptionCollectionFailureEvent(resourceId, e, event.getData().getResourceCrn());
    }

    public abstract Selectable executeOperation(HandlerEvent<StorageConsumptionCollectionHandlerEvent> data) throws Exception;

    public abstract String getOperationName();
}
