package com.sequenceiq.datalake.controller.sdx;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.upgrade.SdxRuntimeUpgradeService;
import com.sequenceiq.datalake.service.upgrade.ccm.SdxCcmUpgradeService;
import com.sequenceiq.sdx.api.endpoint.SdxUpgradeEndpoint;
import com.sequenceiq.sdx.api.model.SdxCcmUpgradeResponse;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;

@Controller
@AccountEntityType(SdxCluster.class)
public class SdxUpgradeController implements SdxUpgradeEndpoint {

    @Inject
    private SdxRuntimeUpgradeService sdxRuntimeUpgradeService;

    @Inject
    private SdxCcmUpgradeService sdxCcmUpgradeService;

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.UPGRADE_DATALAKE)
    public SdxUpgradeResponse upgradeClusterByName(@ResourceName String clusterName, SdxUpgradeRequest request) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (request.isDryRun() || request.isShowAvailableImagesSet()) {
            return sdxRuntimeUpgradeService.checkForUpgradeByName(userCrn, clusterName, request, ThreadBasedUserCrnProvider.getAccountId());
        } else {
            return sdxRuntimeUpgradeService.triggerUpgradeByName(userCrn, clusterName, request, ThreadBasedUserCrnProvider.getAccountId());
        }
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPGRADE_DATALAKE)
    public SdxUpgradeResponse upgradeClusterByCrn(@TenantAwareParam @ResourceCrn String clusterCrn, SdxUpgradeRequest request) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (request.isDryRun() || request.isShowAvailableImagesSet()) {
            return sdxRuntimeUpgradeService.checkForUpgradeByCrn(userCrn, clusterCrn, request, ThreadBasedUserCrnProvider.getAccountId());
        } else {
            return sdxRuntimeUpgradeService.triggerUpgradeByCrn(userCrn, clusterCrn, request, ThreadBasedUserCrnProvider.getAccountId());
        }
    }

    @Override
    @InternalOnly
    public SdxCcmUpgradeResponse upgradeCcm(@TenantAwareParam String environmentCrn) {
        return sdxCcmUpgradeService.upgradeCcm(environmentCrn);
    }

}
