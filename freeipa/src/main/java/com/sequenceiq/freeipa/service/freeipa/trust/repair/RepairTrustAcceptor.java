package com.sequenceiq.freeipa.service.freeipa.trust.repair;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.service.freeipa.user.AcceptResult;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class RepairTrustAcceptor extends OperationAcceptor {

    @Override
    protected OperationType selector() {
        return OperationType.REPAIR_TRUST_SETUP;
    }

    @Override
    public AcceptResult accept(Operation operation) {
        return AcceptResult.accept();
    }
}
