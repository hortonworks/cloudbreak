package com.sequenceiq.freeipa.flow.freeipa.enableselinux;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class FreeIpaModifySeLinuxAcceptor extends OperationAcceptor {

    @Override
    protected OperationType selector() {
        return OperationType.MODIFY_SELINUX_MODE;
    }
}
