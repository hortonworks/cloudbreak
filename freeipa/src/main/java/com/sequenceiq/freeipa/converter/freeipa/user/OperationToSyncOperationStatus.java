package com.sequenceiq.freeipa.converter.freeipa.user;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.entity.Operation;

@Component
public class OperationToSyncOperationStatus implements Converter<Operation, SyncOperationStatus> {

    @Override
    public SyncOperationStatus convert(Operation source) {
        return new SyncOperationStatus(
                source.getOperationId(),
                SyncOperationType.fromOperationType(source.getOperationType()),
                SynchronizationStatus.fromOperationState(source.getStatus()),
                source.getSuccessList(),
                source.getFailureList(),
                source.getError(),
                source.getStartTime(),
                source.getEndTime()
        );
    }
}
