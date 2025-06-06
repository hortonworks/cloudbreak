package com.sequenceiq.freeipa.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_ENVIRONMENT;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.flow.api.model.operation.OperationStatusResponse;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.flow.service.FlowService;
import com.sequenceiq.freeipa.api.v1.operation.OperationV1Endpoint;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.service.operation.FlowOperationService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@Controller
public class OperationV1Controller implements OperationV1Endpoint {

    @Inject
    private OperationService operationService;

    @Inject
    private FlowOperationService flowOperationService;

    @Inject
    private OperationToOperationStatusConverter operationToOperationStatusConverter;

    @Inject
    private FlowService flowService;

    @Inject
    private StackService stackService;

    @Inject
    private CrnService crnService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.GET_OPERATION_STATUS)
    public OperationStatus getOperationStatus(String operationId, @AccountId String accountId) {
        String currentAccountId = Optional.ofNullable(accountId).orElseGet(ThreadBasedUserCrnProvider::getAccountId);
        return operationToOperationStatusConverter.convert(operationService.getOperationForAccountIdAndOperationId(currentAccountId, operationId));
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_ENVIRONMENT)
    public OperationView getOperationProgressByEnvironmentCrn(@ResourceCrn String environmentCrn, boolean detailed) {
        return flowOperationService.getOperationProgressByEnvironmentCrn(environmentCrn, detailed);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_ENVIRONMENT)
    public OperationStatusResponse getFlowOperationStatus(@ResourceCrn String environmentCrn, String flowOperationId) {
        String resourceCrn = stackService.getFreeIpaStackWithMdcContext(environmentCrn, crnService.getCurrentAccountId()).getResourceCrn();
        return flowService.getOperationStatus(resourceCrn, flowOperationId);
    }
}
