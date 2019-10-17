package com.sequenceiq.freeipa.service.freeipa.user;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.repository.OperationRepository;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class SetPasswordAcceptor extends OperationAcceptor {
    @Inject
    public SetPasswordAcceptor(OperationRepository operationRepository) {
        super(operationRepository);
    }

    @Override
    protected OperationType selector() {
        return OperationType.SET_PASSWORD;
    }
}
