package com.sequenceiq.freeipa.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_ENVIRONMENT;

import java.util.Optional;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.freeipa.api.v1.operation.OperationV1Endpoint;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Controller
public class OperationV1Controller implements OperationV1Endpoint {

    @Inject
    private OperationService operationService;

    @Inject
    private OperationToOperationStatusConverter operationToOperationStatusConverter;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.GET_OPERATION_STATUS)
    public OperationStatus getOperationStatus(@NotNull String operationId, @AccountId String accountId) {
        String currentAccountId = Optional.ofNullable(accountId).orElseGet(ThreadBasedUserCrnProvider::getAccountId);
        return operationToOperationStatusConverter.convert(operationService.getOperationForAccountIdAndOperationId(currentAccountId, operationId));
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_ENVIRONMENT)
    public OperationView getOperationProgressByEnvironmentCrn(@ResourceCrn String resourceCrn, boolean detailed) {
        return operationService.getOperationProgressByEnvironmentCrn(resourceCrn, detailed);
    }
}
