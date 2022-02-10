package com.sequenceiq.datalake.controller.sdx;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
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
    public SdxRecoveryResponse recoverClusterByName(@ResourceName String name, @Valid SdxRecoveryRequest recoverSdxClusterRequest) {
        SdxCluster cluster = sdxService.getByNameInAccount(ThreadBasedUserCrnProvider.getUserCrn(), name);
        return sdxRecoverySelectorService.triggerRecovery(cluster, recoverSdxClusterRequest);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.RECOVER_DATALAKE)
    public SdxRecoveryResponse recoverClusterByCrn(@ResourceCrn @TenantAwareParam String crn,
            @Valid SdxRecoveryRequest recoverSdxClusterRequest) {
        SdxCluster cluster = sdxService.getByCrn(ThreadBasedUserCrnProvider.getUserCrn(), crn);
        return sdxRecoverySelectorService.triggerRecovery(cluster, recoverSdxClusterRequest);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.RECOVER_DATALAKE)
    public SdxRecoverableResponse getClusterRecoverableByName(@ResourceName String name) {
        SdxCluster cluster = sdxService.getByNameInAccount(ThreadBasedUserCrnProvider.getUserCrn(), name);
        return sdxRecoverySelectorService.validateRecovery(cluster);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.RECOVER_DATALAKE)
    public SdxRecoverableResponse getClusterRecoverableByCrn(@ResourceCrn @TenantAwareParam String crn) {
        SdxCluster cluster = sdxService.getByCrn(ThreadBasedUserCrnProvider.getUserCrn(), crn);
        return sdxRecoverySelectorService.validateRecovery(cluster);
    }
}
