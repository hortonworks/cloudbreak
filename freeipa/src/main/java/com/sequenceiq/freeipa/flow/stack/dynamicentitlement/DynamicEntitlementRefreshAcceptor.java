package com.sequenceiq.freeipa.flow.stack.dynamicentitlement;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class DynamicEntitlementRefreshAcceptor extends OperationAcceptor {

    @Override
    protected OperationType selector() {
        return OperationType.CHANGE_DYNAMIC_ENTITLEMENTS;
    }
}
