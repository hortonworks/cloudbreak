package com.sequenceiq.freeipa.flow.stack.dynamicentitlement;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.repository.OperationRepository;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class DynamicEntitlementRefreshAcceptor extends OperationAcceptor {
    @Inject
    public DynamicEntitlementRefreshAcceptor(OperationRepository operationRepository) {
        super(operationRepository);
    }

    @Override
    protected OperationType selector() {
        return OperationType.CHANGE_DYNAMIC_ENTITLEMENTS;
    }
}
