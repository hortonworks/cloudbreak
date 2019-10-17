package com.sequenceiq.freeipa.controller;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.sequenceiq.freeipa.api.v1.operation.OperationV1Endpoint;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.service.operation.OperationStatusService;
import com.sequenceiq.freeipa.util.CrnService;

@Controller
public class OperationV1Controller implements OperationV1Endpoint {

    @Inject
    private OperationStatusService operationService;

    @Inject
    private CrnService crnService;

    @Override
    public OperationStatus getOperationStatus(@NotNull String operationId) {
        String accountId = crnService.getCurrentAccountId();
        return operationService.getOperationStatus(operationId, accountId);
    }
}
