package com.sequenceiq.freeipa.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.ROTATE_FREEIPA_SECRETS;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.rotation.service.SecretTypeListService;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaRotationV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeIpaSecretRotationRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeipaSecretTypeResponse;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.rotation.FreeIpaSecretRotationService;

@Controller
@AccountEntityType(Stack.class)
public class FreeIpaRotationV1Controller implements FreeIpaRotationV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaRotationV1Controller.class);

    @Inject
    private FreeIpaSecretRotationService freeIpaSecretRotationService;

    @Inject
    private SecretTypeListService<FreeipaSecretTypeResponse> listService;

    @Override
    @CheckPermissionByResourceCrn(action = ROTATE_FREEIPA_SECRETS)
    public FlowIdentifier rotateSecretsByCrn(@ResourceCrn String environmentCrn, FreeIpaSecretRotationRequest request) {
        String accountIdFrmCrn = Crn.safeFromString(environmentCrn).getAccountId();
        return freeIpaSecretRotationService.rotateSecretsByCrn(accountIdFrmCrn, environmentCrn, request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = ROTATE_FREEIPA_SECRETS)
    public List<FreeipaSecretTypeResponse> listRotatableFreeipaSecretType(@ResourceCrn String environmentCrn) {
        return listService.listRotatableSecretType(environmentCrn, FreeipaSecretTypeResponse.converter());
    }
}
