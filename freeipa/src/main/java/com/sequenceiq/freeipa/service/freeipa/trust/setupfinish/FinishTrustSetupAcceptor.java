package com.sequenceiq.freeipa.service.freeipa.trust.setupfinish;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.service.freeipa.user.AcceptResult;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class FinishTrustSetupAcceptor extends OperationAcceptor {

    @Override
    protected OperationType selector() {
        return OperationType.TRUST_SETUP_FINISH;
    }

    @Override
    public AcceptResult accept(Operation operation) {
        return AcceptResult.accept();
    }
}
