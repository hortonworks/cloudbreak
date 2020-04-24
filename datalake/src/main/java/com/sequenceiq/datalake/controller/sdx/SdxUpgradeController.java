package com.sequenceiq.datalake.controller.sdx;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.service.sdx.SdxUpgradeService;
import com.sequenceiq.datalake.service.upgrade.SdxClusterUpgradeService;
import com.sequenceiq.sdx.api.endpoint.SdxUpgradeEndpoint;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;

@Controller
@AuthorizationResource(type = AuthorizationResourceType.DATALAKE)
public class SdxUpgradeController implements SdxUpgradeEndpoint {

    @Inject
    private SdxUpgradeService sdxUpgradeService;

    @Inject
    private SdxClusterUpgradeService sdxClusterUpgradeService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public SdxUpgradeResponse upgradeClusterByName(String clusterName, SdxUpgradeRequest upgradeSdxClusterRequest) {
        if (Boolean.TRUE.equals(upgradeSdxClusterRequest.isDryRun())) {
            return sdxClusterUpgradeService.checkForClusterUpgradeByName(clusterName, upgradeSdxClusterRequest);
        }
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (Boolean.TRUE.equals(upgradeSdxClusterRequest.getLockComponents())) {
            return sdxUpgradeService.triggerUpgradeByName(userCrn, clusterName);
        } else {
            return sdxClusterUpgradeService.triggerClusterUpgradeByName(userCrn, clusterName, upgradeSdxClusterRequest);
        }
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public SdxUpgradeResponse upgradeClusterByCrn(String clusterCrn, SdxUpgradeRequest upgradeSdxClusterRequest) {
        if (Boolean.TRUE.equals(upgradeSdxClusterRequest.isDryRun())) {
            String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
            return sdxClusterUpgradeService.checkForClusterUpgradeByCrn(userCrn, clusterCrn, upgradeSdxClusterRequest);
        }
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (Boolean.TRUE.equals(upgradeSdxClusterRequest.getLockComponents())) {
            return sdxUpgradeService.triggerUpgradeByCrn(userCrn, clusterCrn);
        } else {
            return sdxClusterUpgradeService.triggerClusterUpgradeByCrn(userCrn, clusterCrn, upgradeSdxClusterRequest);
        }
    }
}
