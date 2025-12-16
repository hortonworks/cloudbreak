package com.sequenceiq.distrox.v1.distrox.controller;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSetRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.ccm.StackCcmUpgradeService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.util.CodUtil;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXUpgradeV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXCcmUpgradeV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeShowAvailableImages;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds.DistroXRdsUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds.DistroXRdsUpgradeV1Response;
import com.sequenceiq.distrox.v1.distrox.converter.UpgradeConverter;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.DistroXUpgradeAvailabilityService;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.DistroXUpgradeService;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.rds.DistroXRdsUpgradeService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

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

    @Inject
    private StackService stackService;

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXRdsUpgradeV1Response upgradeRdsByName(@ResourceName String name, DistroXRdsUpgradeV1Request distroxRdsUpgradeRequest) {
        validateClusterName(name);
        NameOrCrn nameOrCrn = NameOrCrn.ofName(name);
        return upgradeRds(distroxRdsUpgradeRequest, nameOrCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXRdsUpgradeV1Response upgradeRdsByCrn(@ResourceCrn String crn, DistroXRdsUpgradeV1Request distroxRdsUpgradeRequest) {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(crn);
        return upgradeRds(distroxRdsUpgradeRequest, nameOrCrn);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXUpgradeV1Response upgradeClusterByName(@ResourceName String clusterName, DistroXUpgradeV1Request distroxUpgradeRequest) {
        validateClusterName(clusterName);
        NameOrCrn nameOrCrn = NameOrCrn.ofName(clusterName);
        validateCodCluster(nameOrCrn, distroxUpgradeRequest.getShowAvailableImages());
        return upgradeCluster(distroxUpgradeRequest, nameOrCrn, false);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXUpgradeV1Response upgradeClusterByCrn(@ResourceCrn String clusterCrn, DistroXUpgradeV1Request distroxUpgradeRequest) {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(clusterCrn);
        validateCodCluster(nameOrCrn, distroxUpgradeRequest.getShowAvailableImages());
        return upgradeCluster(distroxUpgradeRequest, nameOrCrn, false);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXUpgradeV1Response prepareClusterUpgradeByName(@ResourceName String clusterName, DistroXUpgradeV1Request distroxUpgradeRequest) {
        validateClusterName(clusterName);
        NameOrCrn nameOrCrn = NameOrCrn.ofName(clusterName);
        return upgradeCluster(distroxUpgradeRequest, nameOrCrn, true);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXUpgradeV1Response prepareClusterUpgradeByCrn(@ResourceCrn String clusterCrn, DistroXUpgradeV1Request distroxUpgradeRequest) {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(clusterCrn);
        return upgradeCluster(distroxUpgradeRequest, nameOrCrn, true);
    }

    @Override
    @InternalOnly
    public DistroXUpgradeV1Response upgradeClusterByNameInternal(@ResourceName String clusterName, DistroXUpgradeV1Request distroxUpgradeRequest,
            @InitiatorUserCrn String initiatorUserCrn, Boolean rollingUpgradeEnabled) {
        validateClusterName(clusterName);
        NameOrCrn nameOrCrn = NameOrCrn.ofName(clusterName);
        distroxUpgradeRequest.setRollingUpgradeEnabled(
                Boolean.TRUE.equals(rollingUpgradeEnabled) || Boolean.TRUE.equals(distroxUpgradeRequest.getRollingUpgradeEnabled()));
        UpgradeV4Request request = upgradeConverter.convert(distroxUpgradeRequest, initiatorUserCrn, true);
        return upgradeCluster(request, nameOrCrn, false);
    }

    @Override
    @InternalOnly
    public DistroXUpgradeV1Response upgradeClusterByCrnInternal(@ResourceCrn String clusterCrn, DistroXUpgradeV1Request distroxUpgradeRequest,
            @InitiatorUserCrn String initiatorUserCrn, Boolean rollingUpgradeEnabled) {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(clusterCrn);
        distroxUpgradeRequest.setRollingUpgradeEnabled(
                Boolean.TRUE.equals(rollingUpgradeEnabled) || Boolean.TRUE.equals(distroxUpgradeRequest.getRollingUpgradeEnabled()));
        UpgradeV4Request request = upgradeConverter.convert(distroxUpgradeRequest, initiatorUserCrn, true);
        return upgradeCluster(request, nameOrCrn, false);
    }

    @Override
    @InternalOnly
    public DistroXCcmUpgradeV1Response upgradeCcmByCrnInternal(String crn, @InitiatorUserCrn String initiatorUserCrn) {
        return upgradeConverter.convert(stackCcmUpgradeService.upgradeCcm(NameOrCrn.ofCrn(crn)));
    }

    @Override
    @InternalOnly
    public FlowIdentifier osUpgradeByUpgradeSetsInternal(@ResourceCrn String crn, OrderedOSUpgradeSetRequest orderedOsUpgradeSetRequest) {
        Long workspaceId = restRequestThreadLocalService.getRequestedWorkspaceId();
        return upgradeService.triggerOsUpgradeByUpgradeSets(NameOrCrn.ofCrn(crn), workspaceId, orderedOsUpgradeSetRequest.getImageId(),
                orderedOsUpgradeSetRequest.getOrderedOsUpgradeSets());
    }

    private DistroXUpgradeV1Response upgradeCluster(DistroXUpgradeV1Request distroxUpgradeRequest, NameOrCrn nameOrCrn,
            boolean upgradePreparation) {
        UpgradeV4Request request = upgradeConverter.convert(distroxUpgradeRequest, ThreadBasedUserCrnProvider.getUserCrn(), false);
        return upgradeCluster(request, nameOrCrn, upgradePreparation);
    }

    private DistroXUpgradeV1Response upgradeCluster(UpgradeV4Request request, NameOrCrn nameOrCrn, boolean upgradePreparation) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        Long workspaceId = restRequestThreadLocalService.getRequestedWorkspaceId();
        if (request.isDryRun() || request.isShowAvailableImagesSet()) {
            LOGGER.info("Checking for upgrade for cluster [{}] with request: {}", nameOrCrn, request);
            UpgradeV4Response upgradeV4Response = upgradeAvailabilityService.checkForUpgrade(nameOrCrn, workspaceId, request, userCrn);
            return upgradeConverter.convert(upgradeV4Response);
        } else {
            LOGGER.info("Triggering upgrade for cluster [{}] with request: {}", nameOrCrn, request);
            UpgradeV4Response upgradeV4Response = upgradeService.triggerUpgrade(nameOrCrn, workspaceId, userCrn, request, upgradePreparation);
            return upgradeConverter.convert(upgradeV4Response);
        }
    }

    private DistroXRdsUpgradeV1Response upgradeRds(DistroXRdsUpgradeV1Request distroxRdsUpgradeRequest, NameOrCrn nameOrCrn) {
        return rdsUpgradeService.triggerUpgrade(nameOrCrn, distroxRdsUpgradeRequest);
    }

    private void validateClusterName(String clusterName) {
        stackService.checkLiveStackExistenceByName(clusterName, ThreadBasedUserCrnProvider.getAccountId(), StackType.WORKLOAD);
    }

    private void validateCodCluster(NameOrCrn nameOrCrn, DistroXUpgradeShowAvailableImages showAvailableImages) {
        if (DistroXUpgradeShowAvailableImages.SHOW != showAvailableImages && DistroXUpgradeShowAvailableImages.LATEST_ONLY != showAvailableImages) {
            Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, restRequestThreadLocalService.getRequestedWorkspaceId());
            if (CodUtil.isCodCluster(stack)) {
                throw new BadRequestException("Please note that COD cluster upgrades are supported only through the Operational Database UI or CLI!");
            }
        }
    }

}
