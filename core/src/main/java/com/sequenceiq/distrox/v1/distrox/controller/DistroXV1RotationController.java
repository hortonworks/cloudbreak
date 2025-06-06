package com.sequenceiq.distrox.v1.distrox.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.ROTATE_DH_SECRETS;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.rotation.service.SecretTypeListService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackRotationService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1RotationEndpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXSecretRotationRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXSecretTypeResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Controller
public class DistroXV1RotationController implements DistroXV1RotationEndpoint {

    @Inject
    private StackRotationService stackRotationService;

    @Inject
    private SecretTypeListService<DistroXSecretTypeResponse> listService;

    @Override
    @CheckPermissionByRequestProperty(type = CRN, path = "crn", action = ROTATE_DH_SECRETS)
    public FlowIdentifier rotateSecrets(@RequestObject DistroXSecretRotationRequest request) {
        return stackRotationService.rotateSecrets(request.getCrn(), request.getSecrets(), request.getExecutionType(), request.getAdditionalProperties());
    }

    @Override
    @CheckPermissionByResourceCrn(action = ROTATE_DH_SECRETS)
    public List<DistroXSecretTypeResponse> listRotatableDistroXSecretType(@ResourceCrn String datahubCrn) {
        return listService.listRotatableSecretType(datahubCrn, DistroXSecretTypeResponse.converter());
    }
}
