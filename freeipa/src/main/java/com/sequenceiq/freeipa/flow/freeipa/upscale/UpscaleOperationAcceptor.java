package com.sequenceiq.freeipa.flow.freeipa.upscale;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class UpscaleOperationAcceptor extends OperationAcceptor {

    @Override
    protected OperationType selector() {
        return OperationType.UPSCALE;
    }
}
