package com.sequenceiq.freeipa.flow.freeipa.upscale;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.repository.OperationRepository;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class UpscaleOperationAcceptor extends OperationAcceptor {

    @Inject
    public UpscaleOperationAcceptor(OperationRepository operationRepository) {
        super(operationRepository);
    }

    @Override
    protected OperationType selector() {
        return OperationType.UPSCALE;
    }
}
