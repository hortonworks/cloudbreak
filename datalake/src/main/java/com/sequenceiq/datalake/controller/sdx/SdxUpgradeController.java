package com.sequenceiq.datalake.controller.sdx;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.service.upgrade.SdxRuntimeUpgradeService;
import com.sequenceiq.sdx.api.endpoint.SdxUpgradeEndpoint;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;

@Controller
@AuthorizationResource
public class SdxUpgradeController implements SdxUpgradeEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeController.class);

    @Inject
    private SdxRuntimeUpgradeService sdxRuntimeUpgradeService;

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.UPGRADE_DATALAKE)
    public SdxUpgradeResponse upgradeClusterByName(@ResourceName String clusterName, SdxUpgradeRequest request) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        lockComponentsIfRuntimeUpgradeIsDisabled(request, userCrn, clusterName);
        if (request.isDryRun() || request.isShowAvailableImagesSet()) {
            return sdxRuntimeUpgradeService.checkForUpgradeByName(userCrn, clusterName, request);
        } else {
            return sdxRuntimeUpgradeService.triggerUpgradeByName(userCrn, clusterName, request);
        }
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPGRADE_DATALAKE)
    public SdxUpgradeResponse upgradeClusterByCrn(@ResourceCrn String clusterCrn, SdxUpgradeRequest request) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        lockComponentsIfRuntimeUpgradeIsDisabled(request, userCrn, clusterCrn);
        if (request.isDryRun() || request.isShowAvailableImagesSet()) {
            return sdxRuntimeUpgradeService.checkForUpgradeByCrn(userCrn, clusterCrn, request);
        } else {
            return sdxRuntimeUpgradeService.triggerUpgradeByCrn(userCrn, clusterCrn, request);
        }
    }

    private void lockComponentsIfRuntimeUpgradeIsDisabled(SdxUpgradeRequest request, String userCrn, String clusterNameOrCrn) {
        if (!requestSpecifiesUpgradeType(request) && !sdxRuntimeUpgradeService.isRuntimeUpgradeEnabled(userCrn)) {
            LOGGER.info("Set lock-components since no upgrade type is specified and runtime upgrade is disabled for cluster: {}", clusterNameOrCrn);
            request.setLockComponents(true);
        }
    }

    private boolean requestSpecifiesUpgradeType(SdxUpgradeRequest request) {
        return !(request.isEmpty() || isDryRunOnly(request));
    }

    private boolean isDryRunOnly(SdxUpgradeRequest request) {
        return request.isDryRun() && isEmpty(request.getRuntime()) && isEmpty(request.getImageId()) && !sdxRuntimeUpgradeService.isOsUpgrade(request);
    }
}
