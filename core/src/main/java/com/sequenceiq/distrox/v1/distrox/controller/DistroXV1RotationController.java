package com.sequenceiq.distrox.v1.distrox.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.ROTATE_DH_SECRETS;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;
import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.DATAHUB;
import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.DATALAKE;
import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.ENVIRONMENT;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.RequestObject;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.annotation.ValidMultiSecretType;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackRotationService;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1RotationEndpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXChildResourceMarkingRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXSecretRotationRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXSecretTypeResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Controller
public class DistroXV1RotationController implements DistroXV1RotationEndpoint {

    @Inject
    private StackRotationService stackRotationService;

    @Inject
    private SecretRotationNotificationService notificationService;

    @Override
    @CheckPermissionByRequestProperty(type = CRN, path = "crn", action = ROTATE_DH_SECRETS)
    public FlowIdentifier rotateSecrets(@TenantAwareParam @RequestObject DistroXSecretRotationRequest request) {
        return stackRotationService.rotateSecrets(request.getCrn(), request.getSecrets(), request.getExecutionType(), request.getAdditionalProperties());
    }

    @Override
    @InternalOnly
    public boolean checkOngoingChildrenMultiSecretRotationsByParent(@ValidCrn(resource = { ENVIRONMENT, DATALAKE }) String parentCrn,
            @ValidMultiSecretType String multiSecret,
            @InitiatorUserCrn String initiatorUserCrn) {
        return stackRotationService.checkOngoingChildrenMultiSecretRotations(parentCrn, multiSecret);
    }

    @Override
    @InternalOnly
    public void markMultiClusterChildrenResourcesByParent(@Valid DistroXChildResourceMarkingRequest request,
            @InitiatorUserCrn String initiatorUserCrn) {
        stackRotationService.markMultiClusterChildrenResources(request.getParentCrn(), request.getSecret());
    }

    @Override
    @CheckPermissionByResourceCrn(action = ROTATE_DH_SECRETS)
    public List<DistroXSecretTypeResponse> listRotatableDistroXSecretType(
            @ValidCrn(resource = DATAHUB) @ResourceCrn @NotEmpty @TenantAwareParam String datahubCrn) {
        // further improvement needed to query secret types for resource
        return Arrays.stream(CloudbreakSecretType.values())
                .filter(Predicate.not(CloudbreakSecretType::internal))
                .map(type -> new DistroXSecretTypeResponse(type.value(), notificationService.getMessage(type)))
                .toList();
    }
}
