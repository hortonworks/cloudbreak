package com.sequenceiq.datalake.controller.sdx;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.service.sdx.SdxUpgradeService;
import com.sequenceiq.datalake.service.upgrade.SdxRuntimeUpgradeService;
import com.sequenceiq.sdx.api.endpoint.SdxUpgradeEndpoint;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;

@Controller
@AuthorizationResource(type = AuthorizationResourceType.DATALAKE)
public class SdxUpgradeController implements SdxUpgradeEndpoint {

    @Inject
    private SdxUpgradeService sdxUpgradeService;

    @Inject
    private SdxRuntimeUpgradeService sdxRuntimeUpgradeService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public SdxUpgradeResponse upgradeClusterByName(String clusterName, SdxUpgradeRequest request) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (isDryRun(request)) {
            if (isOsUpgrade(request)) {
                return new SdxUpgradeResponse(sdxUpgradeService.checkForOsUpgradeByName(userCrn, clusterName));
            } else {
                return sdxRuntimeUpgradeService.checkForRuntimeUpgradeByName(userCrn, clusterName, request);
            }
        }
        if (isOsUpgrade(request)) {
            return sdxUpgradeService.triggerOsUpgradeByName(userCrn, clusterName);
        } else {
            return sdxRuntimeUpgradeService.triggerRuntimeUpgradeByName(userCrn, clusterName, request);
        }
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public SdxUpgradeResponse upgradeClusterByCrn(String clusterCrn, SdxUpgradeRequest request) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (isDryRun(request)) {
            if (isOsUpgrade(request)) {
                return new SdxUpgradeResponse(sdxUpgradeService.checkForOsUpgradeByCrn(userCrn, clusterCrn));
            } else {
                return sdxRuntimeUpgradeService.checkForRuntimeUpgradeByCrn(userCrn, clusterCrn, request);
            }
        }
        if (isOsUpgrade(request)) {
            return sdxUpgradeService.triggerOsUpgradeByCrn(userCrn, clusterCrn);
        } else {
            return sdxRuntimeUpgradeService.triggerRuntimeUpgradeByCrn(userCrn, clusterCrn, request);
        }
    }

    private boolean isDryRun(SdxUpgradeRequest request) {
        return Boolean.TRUE.equals(request.isDryRun());
    }

    private boolean isOsUpgrade(SdxUpgradeRequest request) {
        return Boolean.TRUE.equals(request.getLockComponents());
    }
}
