package com.sequenceiq.datalake.controller.sdx;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.upgrade.SdxRuntimeUpgradeService;
import com.sequenceiq.datalake.service.upgrade.ccm.SdxCcmUpgradeService;
import com.sequenceiq.datalake.service.upgrade.database.SdxDatabaseServerUpgradeService;
import com.sequenceiq.sdx.api.endpoint.SdxUpgradeEndpoint;
import com.sequenceiq.sdx.api.model.SdxCcmUpgradeResponse;
import com.sequenceiq.sdx.api.model.SdxUpgradeDatabaseServerRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeDatabaseServerResponse;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;

@Controller
@AccountEntityType(SdxCluster.class)
public class SdxUpgradeController implements SdxUpgradeEndpoint {

    @Inject
    private SdxRuntimeUpgradeService sdxRuntimeUpgradeService;

    @Inject
    private SdxCcmUpgradeService sdxCcmUpgradeService;

    @Inject
    private SdxDatabaseServerUpgradeService sdxDatabaseServerUpgradeService;

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.UPGRADE_DATALAKE)
    public SdxUpgradeResponse upgradeClusterByName(@ResourceName String clusterName, SdxUpgradeRequest request) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (request.isDryRun() || request.isShowAvailableImagesSet()) {
            return sdxRuntimeUpgradeService.checkForUpgradeByName(userCrn, clusterName, request, false);
        } else {
            return sdxRuntimeUpgradeService.triggerUpgradeByName(userCrn, clusterName, request, false);
        }
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPGRADE_DATALAKE)
    public SdxUpgradeResponse upgradeClusterByCrn(@ResourceCrn String clusterCrn, SdxUpgradeRequest request) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (request.isDryRun() || request.isShowAvailableImagesSet()) {
            return sdxRuntimeUpgradeService.checkForUpgradeByCrn(userCrn, clusterCrn, request, false);
        } else {
            return sdxRuntimeUpgradeService.triggerUpgradeByCrn(userCrn, clusterCrn, request, false);
        }
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.UPGRADE_DATALAKE)
    public SdxUpgradeResponse prepareClusterUpgradeByName(@ResourceName String name, SdxUpgradeRequest request) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (request.isDryRun() || request.isShowAvailableImagesSet()) {
            return sdxRuntimeUpgradeService.checkForUpgradeByName(userCrn, name, request, true);
        } else {
            return sdxRuntimeUpgradeService.triggerUpgradeByName(userCrn, name, request, true);
        }
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPGRADE_DATALAKE)
    public SdxUpgradeResponse prepareClusterUpgradeByCrn(@ResourceCrn String crn, SdxUpgradeRequest request) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (request.isDryRun() || request.isShowAvailableImagesSet()) {
            return sdxRuntimeUpgradeService.checkForUpgradeByCrn(userCrn, crn, request, true);
        } else {
            return sdxRuntimeUpgradeService.triggerUpgradeByCrn(userCrn, crn, request, true);
        }
    }

    @Override
    @InternalOnly
    public SdxCcmUpgradeResponse upgradeCcm(@ResourceCrn String environmentCrn, @InitiatorUserCrn String initiatorUserCrn) {
        return sdxCcmUpgradeService.upgradeCcm(environmentCrn);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.UPGRADE_DATALAKE)
    public SdxUpgradeDatabaseServerResponse upgradeDatabaseServerByName(@ResourceName String clusterName,
            SdxUpgradeDatabaseServerRequest sdxUpgradeDatabaseServerRequest) {
        NameOrCrn sdxNameOrCrn = NameOrCrn.ofName(clusterName);
        return sdxDatabaseServerUpgradeService.upgrade(sdxNameOrCrn, sdxUpgradeDatabaseServerRequest.getTargetMajorVersion(),
                Boolean.TRUE.equals(sdxUpgradeDatabaseServerRequest.getForced()));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPGRADE_DATALAKE)
    public SdxUpgradeDatabaseServerResponse upgradeDatabaseServerByCrn(@ResourceCrn String clusterCrn,
            SdxUpgradeDatabaseServerRequest sdxUpgradeDatabaseServerRequest) {
        NameOrCrn sdxNameOrCrn = NameOrCrn.ofCrn(clusterCrn);
        return sdxDatabaseServerUpgradeService.upgrade(sdxNameOrCrn, sdxUpgradeDatabaseServerRequest.getTargetMajorVersion(),
                Boolean.TRUE.equals(sdxUpgradeDatabaseServerRequest.getForced()));
    }

}
