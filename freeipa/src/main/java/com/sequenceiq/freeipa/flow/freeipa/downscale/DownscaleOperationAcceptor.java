package com.sequenceiq.freeipa.flow.freeipa.downscale;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.repository.OperationRepository;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class DownscaleOperationAcceptor extends OperationAcceptor {

    @Inject
    public DownscaleOperationAcceptor(OperationRepository operationRepository) {
        super(operationRepository);
    }

    @Override
    protected OperationType selector() {
        return OperationType.DOWNSCALE;
    }
}
