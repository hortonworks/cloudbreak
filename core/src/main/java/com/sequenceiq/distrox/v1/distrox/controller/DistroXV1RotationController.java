package com.sequenceiq.distrox.v1.distrox.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.ROTATE_DH_SECRETS;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;
import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.DATAHUB;

import java.util.List;
import java.util.function.Predicate;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotEmpty;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretListField;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackRotationService;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1RotationEndpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXSecretRotationRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXSecretTypeResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Controller
public class DistroXV1RotationController implements DistroXV1RotationEndpoint {

    @Inject
    private StackRotationService stackRotationService;

    @Inject
    private SecretRotationNotificationService notificationService;

    @Resource
    private List<SecretType> enabledSecretTypes;

    @Override
    @CheckPermissionByRequestProperty(type = CRN, path = "crn", action = ROTATE_DH_SECRETS)
    public FlowIdentifier rotateSecrets(@RequestObject DistroXSecretRotationRequest request) {
        return stackRotationService.rotateSecrets(request.getCrn(), request.getSecrets(), request.getExecutionType(), request.getAdditionalProperties());
    }

    @Override
    @CheckPermissionByResourceCrn(action = ROTATE_DH_SECRETS)
    public List<DistroXSecretTypeResponse> listRotatableDistroXSecretType(
            @ValidCrn(resource = DATAHUB) @ResourceCrn @NotEmpty String datahubCrn) {
        // further improvement needed to query secret types for resource
        return enabledSecretTypes.stream()
                .filter(Predicate.not(SecretType::internal))
                .map(type -> new DistroXSecretTypeResponse(type.value(),
                        notificationService.getMessage(type, SecretListField.DISPLAY_NAME),
                        notificationService.getMessage(type, SecretListField.DESCRIPTION)))
                .toList();
    }
}
