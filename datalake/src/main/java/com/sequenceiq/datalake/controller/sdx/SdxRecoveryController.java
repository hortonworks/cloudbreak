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
import com.sequenceiq.datalake.service.upgrade.recovery.SdxUpgradeRecoveryService;
import com.sequenceiq.sdx.api.endpoint.SdxRecoveryEndpoint;
import com.sequenceiq.sdx.api.model.SdxRecoverableResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;

@Controller
public class SdxRecoveryController implements SdxRecoveryEndpoint {

    @Inject
    private SdxUpgradeRecoveryService sdxUpgradeRecoveryService;

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.RECOVER_DATALAKE)
    public SdxRecoveryResponse recoverClusterByName(@ResourceName String name, @Valid SdxRecoveryRequest recoverSdxClusterRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxUpgradeRecoveryService.triggerRecovery(userCrn, NameOrCrn.ofName(name), recoverSdxClusterRequest);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.RECOVER_DATALAKE)
    public SdxRecoveryResponse recoverClusterByCrn(@ResourceCrn @TenantAwareParam String crn,
            @Valid SdxRecoveryRequest recoverSdxClusterRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxUpgradeRecoveryService.triggerRecovery(userCrn, NameOrCrn.ofCrn(crn), recoverSdxClusterRequest);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.RECOVER_DATALAKE)
    public SdxRecoverableResponse getClusterRecoverableByName(@ResourceName String name) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxUpgradeRecoveryService.validateRecovery(userCrn, NameOrCrn.ofName(name));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.RECOVER_DATALAKE)
    public SdxRecoverableResponse getClusterRecoverableByCrn(@ResourceCrn @TenantAwareParam String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxUpgradeRecoveryService.validateRecovery(userCrn, NameOrCrn.ofCrn(crn));
    }
}
