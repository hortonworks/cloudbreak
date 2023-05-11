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
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSetRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.service.stack.StackService;
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
    public DistroXRdsUpgradeV1Response upgradeRdsByName(@ResourceName String name, @Valid DistroXRdsUpgradeV1Request distroxRdsUpgradeRequest) {
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
    public DistroXUpgradeV1Response upgradeClusterByName(@ResourceName String clusterName, @Valid DistroXUpgradeV1Request distroxUpgradeRequest) {
        validateClusterName(clusterName);
        NameOrCrn nameOrCrn = NameOrCrn.ofName(clusterName);
        return upgradeCluster(clusterName, distroxUpgradeRequest, nameOrCrn, false);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXUpgradeV1Response upgradeClusterByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB)
    @ResourceCrn String clusterCrn, @Valid DistroXUpgradeV1Request distroxUpgradeRequest) {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(clusterCrn);
        return upgradeCluster(clusterCrn, distroxUpgradeRequest, nameOrCrn, false);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXUpgradeV1Response prepareClusterUpgradeByName(@ResourceName String clusterName, @Valid DistroXUpgradeV1Request distroxUpgradeRequest) {
        validateClusterName(clusterName);
        NameOrCrn nameOrCrn = NameOrCrn.ofName(clusterName);
        return upgradeCluster(clusterName, distroxUpgradeRequest, nameOrCrn, true);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXUpgradeV1Response prepareClusterUpgradeByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB)
    @ResourceCrn String clusterCrn, @Valid DistroXUpgradeV1Request distroxUpgradeRequest) {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(clusterCrn);
        return upgradeCluster(clusterCrn, distroxUpgradeRequest, nameOrCrn, true);
    }

    @Override
    @InternalOnly
    public DistroXUpgradeV1Response upgradeClusterByNameInternal(@ResourceName String clusterName, @Valid DistroXUpgradeV1Request distroxUpgradeRequest,
            @InitiatorUserCrn String initiatorUserCrn, Boolean rollingUpgradeEnabled) {
        validateClusterName(clusterName);
        NameOrCrn nameOrCrn = NameOrCrn.ofName(clusterName);
        InternalUpgradeSettings internalUpgradeSettings = new InternalUpgradeSettings(true,
                upgradeAvailabilityService.isRuntimeUpgradeEnabledByUserCrn(initiatorUserCrn),
                Boolean.TRUE.equals(rollingUpgradeEnabled) || Boolean.TRUE.equals(distroxUpgradeRequest.getRollingUpgradeEnabled()));
        UpgradeV4Request request = upgradeConverter.convert(distroxUpgradeRequest, internalUpgradeSettings);
        return upgradeCluster(clusterName, request, nameOrCrn, false);
    }

    @Override
    @InternalOnly
    public DistroXUpgradeV1Response upgradeClusterByCrnInternal(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB)
    @ResourceCrn String clusterCrn, @Valid DistroXUpgradeV1Request distroxUpgradeRequest, @InitiatorUserCrn String initiatorUserCrn,
            Boolean rollingUpgradeEnabled) {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(clusterCrn);
        InternalUpgradeSettings internalUpgradeSettings = new InternalUpgradeSettings(true,
                upgradeAvailabilityService.isRuntimeUpgradeEnabledByUserCrn(initiatorUserCrn),
                Boolean.TRUE.equals(rollingUpgradeEnabled) || Boolean.TRUE.equals(distroxUpgradeRequest.getRollingUpgradeEnabled()));
        UpgradeV4Request request = upgradeConverter.convert(distroxUpgradeRequest, internalUpgradeSettings);
        return upgradeCluster(clusterCrn, request, nameOrCrn, false);
    }

    @Override
    @InternalOnly
    public DistroXCcmUpgradeV1Response upgradeCcmByCrnInternal(@NotEmpty @ValidCrn(resource = CrnResourceDescriptor.DATAHUB) String crn,
            @InitiatorUserCrn @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER}) @NotEmpty String initiatorUserCrn) {
        return upgradeConverter.convert(stackCcmUpgradeService.upgradeCcm(NameOrCrn.ofCrn(crn)));
    }

    @Override
    @InternalOnly
    public FlowIdentifier osUpgradeByUpgradeSetsInternal(@TenantAwareParam @ResourceCrn String crn,
            OrderedOSUpgradeSetRequest orderedOsUpgradeSetRequest) {
        Long workspaceId = restRequestThreadLocalService.getRequestedWorkspaceId();
        return upgradeService.triggerOsUpgradeByUpgradeSets(NameOrCrn.ofCrn(crn), workspaceId, orderedOsUpgradeSetRequest.getImageId(),
                orderedOsUpgradeSetRequest.getOrderedOsUpgradeSets());
    }

    private DistroXUpgradeV1Response upgradeCluster(String clusterNameOrCrn, DistroXUpgradeV1Request distroxUpgradeRequest, NameOrCrn nameOrCrn,
            boolean upgradePreparation) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        boolean dataHubRuntimeUpgradeEnabled = upgradeAvailabilityService.isRuntimeUpgradeEnabledByAccountId(accountId);
        UpgradeV4Request request = upgradeConverter.convert(distroxUpgradeRequest, new InternalUpgradeSettings(false, dataHubRuntimeUpgradeEnabled,
                Boolean.TRUE.equals(distroxUpgradeRequest.getRollingUpgradeEnabled())));
        return upgradeCluster(clusterNameOrCrn, request, nameOrCrn, upgradePreparation);
    }

    private DistroXUpgradeV1Response upgradeCluster(String clusterNameOrCrn, UpgradeV4Request request, NameOrCrn nameOrCrn, boolean upgradePreparation) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
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

    private void validateClusterName(String clusterName) {
        stackService.checkLiveStackExistenceByName(clusterName, restRequestThreadLocalService.getAccountId(), StackType.WORKLOAD);
    }

}
