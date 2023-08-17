package com.sequenceiq.freeipa.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.ROTATE_FREEIPA_SECRETS;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.RequestObject;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.MultiClusterRotationService;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaRotationV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeIpaMultiSecretRotationRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeIpaSecretRotationRequest;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.rotation.FreeIpaSecretRotationService;

@Controller
@AccountEntityType(Stack.class)
public class FreeIpaRotationV1Controller implements FreeIpaRotationV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaRotationV1Controller.class);

    @Inject
    private FreeIpaSecretRotationService freeIpaSecretRotationService;

    @Inject
    private MultiClusterRotationService multiClusterRotationService;

    @Override
    @CheckPermissionByResourceCrn(action = ROTATE_FREEIPA_SECRETS)
    public FlowIdentifier rotateSecretsByCrn(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @ResourceCrn @NotEmpty @TenantAwareParam String environmentCrn,
            @Valid @NotNull FreeIpaSecretRotationRequest request) {
        String accountId = Crn.safeFromString(environmentCrn).getAccountId();
        return freeIpaSecretRotationService.rotateSecretsByCrn(accountId, environmentCrn, request);
    }

    @Deprecated
    @Override
    @CheckPermissionByRequestProperty(type = CRN, path = "crn", action = ROTATE_FREEIPA_SECRETS)
    public FlowIdentifier rotateMultiSecretsByCrn(@Valid @NotNull @RequestObject FreeIpaMultiSecretRotationRequest request,
            @InitiatorUserCrn String initiatorUserCrn) {
        throw new NotImplementedException("Deprecated Rotation API!");
    }
}
