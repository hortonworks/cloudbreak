package com.sequenceiq.freeipa.flow.freeipa.enableselinux;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.repository.OperationRepository;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class FreeIpaEnableSeLinuxAcceptor extends OperationAcceptor {

    @Inject
    public FreeIpaEnableSeLinuxAcceptor(OperationRepository operationRepository) {
        super(operationRepository);
    }

    @Override
    protected OperationType selector() {
        return OperationType.MODIFY_SELINUX_MODE;
    }
}
