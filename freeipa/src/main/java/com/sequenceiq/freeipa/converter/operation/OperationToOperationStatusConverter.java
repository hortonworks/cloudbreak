package com.sequenceiq.freeipa.converter.operation;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.entity.Operation;

@Component
public class OperationToOperationStatusConverter implements Converter<Operation, OperationStatus> {

    @Override
    public OperationStatus convert(Operation source) {
        return new OperationStatus(
                source.getOperationId(),
                source.getOperationType(),
                source.getStatus(),
                source.getSuccessList(),
                source.getFailureList(),
                source.getError(),
                source.getStartTime(),
                source.getEndTime()
        );
    }
}
