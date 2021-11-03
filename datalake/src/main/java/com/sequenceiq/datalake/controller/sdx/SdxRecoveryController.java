package com.sequenceiq.datalake.controller.sdx;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.datalake.service.recovery.SdxRecoveryService;
import com.sequenceiq.sdx.api.endpoint.SdxRecoveryEndpoint;
import com.sequenceiq.sdx.api.model.SdxRecoverableResponse;
import com.sequenceiq.sdx.api.model.UpgradeRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;

@Controller
public class SdxRecoveryController implements SdxRecoveryEndpoint {

    @Inject
    private SdxRecoveryService sdxRecoveryService;

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.RECOVER_DATALAKE)
    public SdxRecoveryResponse recoverClusterByName(@ResourceName String name, @Valid UpgradeRecoveryRequest recoverSdxClusterRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxRecoveryService.triggerRecovery(userCrn, NameOrCrn.ofName(name), recoverSdxClusterRequest);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.RECOVER_DATALAKE)
    public SdxRecoveryResponse recoverClusterByCrn(@ResourceCrn @TenantAwareParam String crn,
            @Valid UpgradeRecoveryRequest recoverSdxClusterRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxRecoveryService.triggerRecovery(userCrn, NameOrCrn.ofCrn(crn), recoverSdxClusterRequest);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.RECOVER_DATALAKE)
    public SdxRecoverableResponse getClusterRecoverableByName(@ResourceName String name) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxRecoveryService.validateRecovery(userCrn, NameOrCrn.ofName(name));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.RECOVER_DATALAKE)
    public SdxRecoverableResponse getClusterRecoverableByCrn(@ResourceCrn @TenantAwareParam String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxRecoveryService.validateRecovery(userCrn, NameOrCrn.ofCrn(crn));
    }
}
