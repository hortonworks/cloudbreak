package com.sequenceiq.datalake.controller.sdx;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.UPGRADE_DATALAKE;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.EncryptionProfileService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.endpoint.SdxEncryptionProfileEndpoint;

@Controller
public class SdxEncryptionProfileController implements SdxEncryptionProfileEndpoint {

    @Inject
    private EncryptionProfileService encryptionProfileService;

    @Inject
    private SdxService sdxService;

    @Override
    @CheckPermissionByResourceName(action = UPGRADE_DATALAKE)
    public FlowIdentifier enableEncryptionProfileByName(@ResourceName String name, String encryptionProfileNameOrCrn) {
        SdxCluster sdxCluster = sdxService.getByNameInAccount(ThreadBasedUserCrnProvider.getUserCrn(), name);
        return encryptionProfileService.enableEncryptionProfileByCrn(sdxCluster.getCrn(), encryptionProfileNameOrCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = UPGRADE_DATALAKE)
    public FlowIdentifier enableEncryptionProfileByCrn(@ResourceCrn String crn, String encryptionProfileNameOrCrn) {
        return encryptionProfileService.enableEncryptionProfileByCrn(crn, encryptionProfileNameOrCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPGRADE_DATALAKE)
    public FlowIdentifier disableEncryptionProfileByCrn(@ResourceCrn String crn) {
        return encryptionProfileService.disableEncryptionProfile(crn);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.UPGRADE_DATALAKE)
    public FlowIdentifier disableEncryptionProfileByName(@ResourceName String name) {
        SdxCluster sdxCluster = sdxService.getByNameInAccount(ThreadBasedUserCrnProvider.getUserCrn(), name);
        return encryptionProfileService.disableEncryptionProfile(sdxCluster.getCrn());
    }
}
