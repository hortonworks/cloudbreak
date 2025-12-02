package com.sequenceiq.freeipa.service.upgrade;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class UpgradeOperationAcceptor extends OperationAcceptor {

    @Override
    protected OperationType selector() {
        return OperationType.UPGRADE;
    }
}
