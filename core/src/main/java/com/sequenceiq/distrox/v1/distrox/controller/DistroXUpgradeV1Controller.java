package com.sequenceiq.distrox.v1.distrox.controller;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

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
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.service.upgrade.ccm.StackCcmUpgradeService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXUpgradeV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXCcmUpgradeV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds.DistroXRdsUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds.DistroXRdsUpgradeV1Response;
import com.sequenceiq.distrox.v1.distrox.converter.UpgradeConverter;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.DistroXUpgradeAvailabilityService;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.DistroXUpgradeService;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.rds.DistroXRdsUpgradeService;

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
    private DistroXRdsUpgradeService rdsUpgradeService;

    @Inject
    private StackCcmUpgradeService stackCcmUpgradeService;

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXUpgradeV1Response upgradeClusterByName(@ResourceName String clusterName, @Valid DistroXUpgradeV1Request distroxUpgradeRequest) {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(clusterName);
        return upgradeCluster(clusterName, distroxUpgradeRequest, nameOrCrn, false);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXUpgradeV1Response upgradeClusterByCrn(@ResourceCrn String clusterCrn, @Valid DistroXUpgradeV1Request distroxUpgradeRequest) {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(clusterCrn);
        return upgradeCluster(clusterCrn, distroxUpgradeRequest, nameOrCrn, false);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXUpgradeV1Response prepareClusterUpgradeByName(@ResourceName String clusterName, @Valid DistroXUpgradeV1Request distroxUpgradeRequest) {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(clusterName);
        return upgradeCluster(clusterName, distroxUpgradeRequest, nameOrCrn, true);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXUpgradeV1Response prepareClusterUpgradeByCrn(@ResourceCrn String clusterCrn, @Valid DistroXUpgradeV1Request distroxUpgradeRequest) {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(clusterCrn);
        return upgradeCluster(clusterCrn, distroxUpgradeRequest, nameOrCrn, true);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXRdsUpgradeV1Response upgradeRdsByName(@ResourceName String name, @Valid DistroXRdsUpgradeV1Request distroxRdsUpgradeRequest) {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(name);
        return upgradeRds(distroxRdsUpgradeRequest, nameOrCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXRdsUpgradeV1Response upgradeRdsByCrn(@ResourceCrn String crn, @Valid DistroXRdsUpgradeV1Request distroxRdsUpgradeRequest) {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(crn);
        return upgradeRds(distroxRdsUpgradeRequest, nameOrCrn);
    }

    @Override
    @InternalOnly
    public DistroXUpgradeV1Response upgradeClusterByNameInternal(@ResourceName String clusterName, @Valid DistroXUpgradeV1Request distroxUpgradeRequest,
            @InitiatorUserCrn String initiatorUserCrn) {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(clusterName);
        boolean dataHubRuntimeUpgradeEnabled = upgradeAvailabilityService.isRuntimeUpgradeEnabledByUserCrn(initiatorUserCrn);
        boolean dataHubOsUpgradeEntitled = upgradeAvailabilityService.isOsUpgradeEnabledByUserCrn(initiatorUserCrn);
        return upgradeCluster(clusterName, distroxUpgradeRequest, nameOrCrn, new InternalUpgradeSettings(true, dataHubRuntimeUpgradeEnabled,
                dataHubOsUpgradeEntitled), false);
    }

    @Override
    @InternalOnly
    public DistroXUpgradeV1Response upgradeClusterByCrnInternal(@ResourceCrn String clusterCrn, @Valid DistroXUpgradeV1Request distroxUpgradeRequest,
            @InitiatorUserCrn String initiatorUserCrn) {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(clusterCrn);
        boolean dataHubRuntimeUpgradeEnabled = upgradeAvailabilityService.isRuntimeUpgradeEnabledByUserCrn(initiatorUserCrn);
        boolean dataHubOsUpgradeEntitled = upgradeAvailabilityService.isOsUpgradeEnabledByUserCrn(initiatorUserCrn);
        return upgradeCluster(clusterCrn, distroxUpgradeRequest, nameOrCrn, new InternalUpgradeSettings(true, dataHubRuntimeUpgradeEnabled,
                dataHubOsUpgradeEntitled), false);
    }

    @Override
    @InternalOnly
    public DistroXCcmUpgradeV1Response upgradeCcmByCrnInternal(@NotEmpty @ValidCrn(resource = CrnResourceDescriptor.DATAHUB) String crn,
            @InitiatorUserCrn @ValidCrn(resource = CrnResourceDescriptor.USER) @NotEmpty String initiatorUserCrn) {
        return upgradeConverter.convert(stackCcmUpgradeService.upgradeCcm(NameOrCrn.ofCrn(crn)));
    }

    private DistroXUpgradeV1Response upgradeCluster(String clusterNameOrCrn, DistroXUpgradeV1Request distroxUpgradeRequest, NameOrCrn nameOrCrn,
            boolean upgradePreparation) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        boolean dataHubRuntimeUpgradeEnabled = upgradeAvailabilityService.isRuntimeUpgradeEnabledByAccountId(accountId);
        boolean dataHubOsUpgradeEntitled = upgradeAvailabilityService.isOsUpgradeEnabledByAccountId(accountId);
        return upgradeCluster(clusterNameOrCrn, distroxUpgradeRequest, nameOrCrn, new InternalUpgradeSettings(false, dataHubRuntimeUpgradeEnabled,
                dataHubOsUpgradeEntitled), upgradePreparation);
    }

    private DistroXUpgradeV1Response upgradeCluster(String clusterNameOrCrn, DistroXUpgradeV1Request distroxUpgradeRequest, NameOrCrn nameOrCrn,
            InternalUpgradeSettings internalUpgradeSettings, boolean upgradePreparation) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        UpgradeV4Request request = upgradeConverter.convert(distroxUpgradeRequest, internalUpgradeSettings);
        Long workspaceId = restRequestThreadLocalService.getRequestedWorkspaceId();
        if (request.isDryRun() || request.isShowAvailableImagesSet()) {
            LOGGER.info("Checking for upgrade for cluster [{}] with request: {}", clusterNameOrCrn, request);
            UpgradeV4Response upgradeV4Response = upgradeAvailabilityService.checkForUpgrade(nameOrCrn, workspaceId, request, userCrn);
            return upgradeConverter.convert(upgradeV4Response);
        } else {
            LOGGER.info("Triggering upgrade for cluster [{}] with request: {}", clusterNameOrCrn, request);
            UpgradeV4Response upgradeV4Response = upgradeService.triggerUpgrade(nameOrCrn, workspaceId, userCrn, request, upgradePreparation);
            return upgradeConverter.convert(upgradeV4Response);
        }
    }

    private DistroXRdsUpgradeV1Response upgradeRds(DistroXRdsUpgradeV1Request distroxRdsUpgradeRequest, NameOrCrn nameOrCrn) {
        return rdsUpgradeService.triggerUpgrade(nameOrCrn, distroxRdsUpgradeRequest);
    }
}
