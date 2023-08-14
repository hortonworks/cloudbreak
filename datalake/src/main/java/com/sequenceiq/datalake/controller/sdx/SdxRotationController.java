package com.sequenceiq.datalake.controller.sdx;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.ROTATE_DL_SECRETS;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;
import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.ENVIRONMENT;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.RequestObject;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.rotation.annotation.ValidMultiSecretType;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.rotation.SdxRotationService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.endpoint.SdxRotationEndpoint;
import com.sequenceiq.sdx.api.model.SdxMultiSecretRotationRequest;
import com.sequenceiq.sdx.api.model.SdxSecretRotationRequest;

@Controller
@AccountEntityType(SdxCluster.class)
public class SdxRotationController implements SdxRotationEndpoint {

    @Inject
    private SdxRotationService sdxRotationService;

    @Override
    @CheckPermissionByRequestProperty(type = CRN, path = "crn", action = ROTATE_DL_SECRETS)
    public FlowIdentifier rotateSecrets(@RequestObject SdxSecretRotationRequest request) {
        return sdxRotationService.triggerSecretRotation(request.getCrn(), request.getSecrets(), request.getExecutionType());
    }

    @Deprecated
    @Override
    @CheckPermissionByRequestProperty(type = CRN, path = "crn", action = ROTATE_DL_SECRETS)
    public FlowIdentifier rotateMultiSecrets(@RequestObject SdxMultiSecretRotationRequest request) {
        throw new NotImplementedException("Deprecated Rotation API!");
    }

    @Override
    @InternalOnly
    public boolean checkOngoingMultiSecretChildrenRotations(@ValidCrn(resource = ENVIRONMENT) String parentCrn,
            @ValidMultiSecretType String secret,
            @InitiatorUserCrn String initiatorUserCrn) {
        return sdxRotationService.checkOngoingMultiSecretChildrenRotations(parentCrn, secret);
    }

    @Override
    @InternalOnly
    public void markMultiClusterChildrenResources(@ValidCrn(resource = ENVIRONMENT) String parentCrn,
            @ValidMultiSecretType String secret,
            @InitiatorUserCrn String initiatorUserCrn) {
        sdxRotationService.markMultiClusterChildrenResources(parentCrn, secret);
    }
}
