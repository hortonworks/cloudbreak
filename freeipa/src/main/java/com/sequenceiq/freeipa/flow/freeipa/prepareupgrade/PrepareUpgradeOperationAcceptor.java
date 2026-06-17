package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class PrepareUpgradeOperationAcceptor extends OperationAcceptor {

    @Override
    protected OperationType selector() {
        return OperationType.PREPARE_UPGRADE;
    }
}
