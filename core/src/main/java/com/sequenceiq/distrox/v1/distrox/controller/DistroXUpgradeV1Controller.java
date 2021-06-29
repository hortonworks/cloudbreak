package com.sequenceiq.distrox.v1.distrox.controller;

import javax.inject.Inject;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXUpgradeV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Response;
import com.sequenceiq.distrox.v1.distrox.converter.UpgradeConverter;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.ComponentLocker;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.DistroXUpgradeAvailabilityService;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.DistroXUpgradeService;

@Controller
public class DistroXUpgradeV1Controller implements DistroXUpgradeV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXUpgradeV1Controller.class);

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private UpgradeConverter upgradeConverter;

    @Inject
    private DistroXUpgradeAvailabilityService upgradeAvailabilityService;

    @Inject
    private DistroXUpgradeService upgradeService;

    @Inject
    private ComponentLocker componentLocker;

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXUpgradeV1Response upgradeClusterByName(@ResourceName String clusterName, @Valid DistroXUpgradeV1Request distroxUpgradeRequest) {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(clusterName);
        return upgradeCluster(clusterName, distroxUpgradeRequest, nameOrCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXUpgradeV1Response upgradeClusterByCrn(@ResourceCrn String clusterCrn, @Valid DistroXUpgradeV1Request distroxUpgradeRequest) {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(clusterCrn);
        return upgradeCluster(clusterCrn, distroxUpgradeRequest, nameOrCrn);
    }

    @Override
    @InternalOnly
    public DistroXUpgradeV1Response upgradeClusterByNameInternal(@ResourceName String clusterName, @Valid DistroXUpgradeV1Request distroxUpgradeRequest,
            @InitiatorUserCrn String initiatorUserCrn) {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(clusterName);
        return upgradeCluster(clusterName, distroxUpgradeRequest, nameOrCrn, new InternalUpgradeSettings(true));
    }

    @Override
    @InternalOnly
    public DistroXUpgradeV1Response upgradeClusterByCrnInternal(@ResourceCrn String clusterCrn, @Valid DistroXUpgradeV1Request distroxUpgradeRequest,
            @InitiatorUserCrn String initiatorUserCrn) {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(clusterCrn);
        return upgradeCluster(clusterCrn, distroxUpgradeRequest, nameOrCrn, new InternalUpgradeSettings(true));
    }

    private DistroXUpgradeV1Response upgradeCluster(String clusterNameOrCrn, DistroXUpgradeV1Request distroxUpgradeRequest, NameOrCrn nameOrCrn) {
        return upgradeCluster(clusterNameOrCrn, distroxUpgradeRequest, nameOrCrn, new InternalUpgradeSettings());
    }

    private DistroXUpgradeV1Response upgradeCluster(String clusterNameOrCrn, DistroXUpgradeV1Request distroxUpgradeRequest, NameOrCrn nameOrCrn,
            InternalUpgradeSettings internalUpgradeSettings) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        DistroXUpgradeV1Request modifiedRequest = componentLocker.lockComponentsIfRuntimeUpgradeIsDisabled(distroxUpgradeRequest, userCrn, clusterNameOrCrn);
        UpgradeV4Request request = upgradeConverter.convert(modifiedRequest, internalUpgradeSettings);
        Long workspaceId = restRequestThreadLocalService.getRequestedWorkspaceId();
        if (request.isDryRun() || request.isShowAvailableImagesSet()) {
            LOGGER.info("Checking for upgrade for cluster [{}] with request: {}", clusterNameOrCrn, request);
            UpgradeV4Response upgradeV4Response = upgradeAvailabilityService.checkForUpgrade(nameOrCrn, workspaceId, request, userCrn);
            return upgradeConverter.convert(upgradeV4Response);
        } else {
            LOGGER.info("Triggering upgrade for cluster [{}] with request: {}", clusterNameOrCrn, request);
            UpgradeV4Response upgradeV4Response = upgradeService.triggerUpgrade(nameOrCrn, workspaceId, userCrn, request);
            return upgradeConverter.convert(upgradeV4Response);
        }
    }
}
