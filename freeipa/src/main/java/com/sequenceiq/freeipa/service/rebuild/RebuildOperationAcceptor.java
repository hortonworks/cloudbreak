package com.sequenceiq.freeipa.service.rebuild;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.repository.OperationRepository;
import com.sequenceiq.freeipa.service.freeipa.user.AcceptResult;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class RebuildOperationAcceptor extends OperationAcceptor {

    @Inject
    public RebuildOperationAcceptor(OperationRepository operationRepository) {
        super(operationRepository);
    }

    @Override
    protected OperationType selector() {
        return OperationType.REBUILD;
    }

    @Override
    public AcceptResult accept(Operation operation) {
        return AcceptResult.accept();
    }
}
