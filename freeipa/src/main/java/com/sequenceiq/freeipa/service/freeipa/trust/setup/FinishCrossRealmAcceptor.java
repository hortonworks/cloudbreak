package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.repository.OperationRepository;
import com.sequenceiq.freeipa.service.freeipa.user.AcceptResult;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class FinishCrossRealmAcceptor extends OperationAcceptor {

    @Inject
    public FinishCrossRealmAcceptor(OperationRepository operationRepository) {
        super(operationRepository);
    }

    @Override
    protected OperationType selector() {
        return OperationType.TRUST_SETUP_FINISH;
    }

    @Override
    public AcceptResult accept(Operation operation) {
        return AcceptResult.accept();
    }
}
