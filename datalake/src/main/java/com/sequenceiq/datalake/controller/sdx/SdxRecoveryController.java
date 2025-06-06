package com.sequenceiq.datalake.controller.sdx;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.recovery.SdxRecoverySelectorService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.sdx.api.endpoint.SdxRecoveryEndpoint;
import com.sequenceiq.sdx.api.model.SdxRecoverableResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;

@Controller
public class SdxRecoveryController implements SdxRecoveryEndpoint {

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxRecoverySelectorService sdxRecoverySelectorService;

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.RECOVER_DATALAKE)
    public SdxRecoveryResponse recoverClusterByName(@ResourceName String name, SdxRecoveryRequest recoverSdxClusterRequest) {
        SdxCluster cluster = sdxService.getByNameInAccountAllowDetached(ThreadBasedUserCrnProvider.getUserCrn(), name);
        return sdxRecoverySelectorService.triggerRecovery(cluster, recoverSdxClusterRequest);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.RECOVER_DATALAKE)
    public SdxRecoveryResponse recoverClusterByCrn(@ResourceCrn String crn, SdxRecoveryRequest recoverSdxClusterRequest) {
        SdxCluster cluster = sdxService.getByCrn(ThreadBasedUserCrnProvider.getUserCrn(), crn);
        return sdxRecoverySelectorService.triggerRecovery(cluster, recoverSdxClusterRequest);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.RECOVER_DATALAKE)
    public SdxRecoverableResponse getClusterRecoverableByName(@ResourceName String name) {
        SdxCluster cluster = sdxService.getByNameInAccountAllowDetached(ThreadBasedUserCrnProvider.getUserCrn(), name);
        return sdxRecoverySelectorService.validateRecovery(cluster);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.RECOVER_DATALAKE)
    public SdxRecoverableResponse getClusterRecoverableByCrn(@ResourceCrn String crn) {
        SdxCluster cluster = sdxService.getByCrn(ThreadBasedUserCrnProvider.getUserCrn(), crn);
        return sdxRecoverySelectorService.validateRecovery(cluster);
    }
}
