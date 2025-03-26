package com.sequenceiq.datalake.controller.sdx;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.ROTATE_DL_SECRETS;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;
import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.VM_DATALAKE;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotEmpty;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.rotation.service.SecretTypeListService;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.rotation.SdxRotationService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.endpoint.SdxRotationEndpoint;
import com.sequenceiq.sdx.api.model.SdxSecretRotationRequest;
import com.sequenceiq.sdx.api.model.SdxSecretTypeResponse;

@Controller
@AccountEntityType(SdxCluster.class)
public class SdxRotationController implements SdxRotationEndpoint {

    @Inject
    private SdxRotationService sdxRotationService;

    @Inject
    private SecretTypeListService<SdxSecretTypeResponse> listService;

    @Override
    @CheckPermissionByRequestProperty(type = CRN, path = "crn", action = ROTATE_DL_SECRETS)
    public FlowIdentifier rotateSecrets(@RequestObject SdxSecretRotationRequest request) {
        return sdxRotationService.triggerSecretRotation(request.getCrn(), request.getSecrets(), request.getExecutionType(), request.getAdditionalProperties());
    }

    @Override
    @CheckPermissionByResourceCrn(action = ROTATE_DL_SECRETS)
    public List<SdxSecretTypeResponse> listRotatableSdxSecretType(@ValidCrn(resource = VM_DATALAKE) @ResourceCrn @NotEmpty String datalakeCrn) {
        return listService.listRotatableSecretType(datalakeCrn, SdxSecretTypeResponse.converter());
    }
}
