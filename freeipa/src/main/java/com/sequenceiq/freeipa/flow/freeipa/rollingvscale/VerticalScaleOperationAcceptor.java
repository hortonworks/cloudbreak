package com.sequenceiq.freeipa.flow.freeipa.rollingvscale;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class VerticalScaleOperationAcceptor extends OperationAcceptor {

    @Override
    protected OperationType selector() {
        return OperationType.VERTICAL_SCALE;
    }
}
