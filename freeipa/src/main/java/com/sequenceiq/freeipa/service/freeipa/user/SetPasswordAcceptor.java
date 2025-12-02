package com.sequenceiq.freeipa.service.freeipa.user;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class SetPasswordAcceptor extends OperationAcceptor {

    @Override
    protected OperationType selector() {
        return OperationType.SET_PASSWORD;
    }
}
