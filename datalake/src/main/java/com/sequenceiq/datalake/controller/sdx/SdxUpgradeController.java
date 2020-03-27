package com.sequenceiq.datalake.controller.sdx;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeOptionsV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.service.sdx.SdxUpgradeService;
import com.sequenceiq.datalake.service.upgrade.SdxClusterUpgradeService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.endpoint.SdxUpgradeEndpoint;

@Controller
@AuthorizationResource(type = AuthorizationResourceType.DATALAKE)
public class SdxUpgradeController implements SdxUpgradeEndpoint {

    @Inject
    private SdxUpgradeService sdxUpgradeService;

    @Inject
    private SdxClusterUpgradeService sdxClusterUpgradeService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public UpgradeOptionV4Response checkForUpgradeByName(String clusterName) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxUpgradeService.checkForUpgradeByName(userCrn, clusterName);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public UpgradeOptionV4Response checkForUpgradeByCrn(String clusterCrn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxUpgradeService.checkForUpgradeByCrn(userCrn, clusterCrn);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public FlowIdentifier upgradeClusterByName(String clusterName) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxUpgradeService.triggerUpgradeByName(userCrn, clusterName);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public FlowIdentifier upgradeClusterByCrn(String clusterCrn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxUpgradeService.triggerUpgradeByCrn(userCrn, clusterCrn);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public UpgradeOptionsV4Response checkForClusterUpgradeByName(String name) {
        return sdxClusterUpgradeService.checkForClusterUpgradeByName(name);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public UpgradeOptionsV4Response checkForClusterUpgradeByCrn(String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxClusterUpgradeService.checkForClusterUpgradeByCrn(crn, userCrn);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public FlowIdentifier upgradeClusterByName(String name, String imageId) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxClusterUpgradeService.triggerClusterUpgradeByName(userCrn, name, imageId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public FlowIdentifier upgradeClusterByCrn(String crn, String imageId) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxClusterUpgradeService.triggerClusterUpgradeByCrn(userCrn, crn, imageId);
    }
}
