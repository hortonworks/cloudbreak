package com.sequenceiq.freeipa.controller;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.freeipa.api.v1.operation.OperationV1Endpoint;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Controller
@AuthorizationResource(type = AuthorizationResourceType.ENVIRONMENT)
public class OperationV1Controller implements OperationV1Endpoint {

    @Inject
    private OperationService operationService;

    @Inject
    private OperationToOperationStatusConverter operationToOperationStatusConverter;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public OperationStatus getOperationStatus(@NotNull String operationId) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return operationToOperationStatusConverter.convert(operationService.getOperationForAccountIdAndOperationId(accountId, operationId));
    }
}
