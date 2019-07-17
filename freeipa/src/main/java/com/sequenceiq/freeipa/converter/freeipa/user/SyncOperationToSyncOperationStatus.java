package com.sequenceiq.freeipa.converter.freeipa.user;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.entity.SyncOperation;

@Component
public class SyncOperationToSyncOperationStatus  implements Converter<SyncOperation, SyncOperationStatus> {

    @Override
    public SyncOperationStatus convert(SyncOperation source) {
        return new SyncOperationStatus(
                source.getOperationId(),
                source.getSyncOperationType(),
                source.getStatus(),
                source.getSuccessList(),
                source.getFailureList(),
                source.getError(),
                source.getStartTime(),
                source.getEndTime()
        );
    }
}
