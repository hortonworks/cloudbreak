package com.sequenceiq.distrox.v1.distrox.controller;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceCrnList;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSetRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.ccm.StackCcmUpgradeService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXUpgradeV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXCcmUpgradeV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds.DistroXDatabaseUpgradeStatus;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds.DistroXRdsUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds.DistroXRdsUpgradeV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.reinit.DistroXUpgradeReinitiableV1Response;
import com.sequenceiq.distrox.v1.distrox.converter.UpgradeConverter;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.DistroXUpgradeService;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.rds.DistroXRdsUpgradeService;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.rds.DistroXRdsUpgradeStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Controller
public class DistroXUpgradeV1Controller implements DistroXUpgradeV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXUpgradeV1Controller.class);

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private UpgradeConverter upgradeConverter;

    @Inject
    private DistroXRdsUpgradeService rdsUpgradeService;

    @Inject
    private DistroXRdsUpgradeStatusService rdsUpgradeStatusService;

    @Inject
    private StackCcmUpgradeService stackCcmUpgradeService;

    @Inject
    private StackService stackService;

    @Inject
    private DistroXUpgradeService distroXUpgradeService;

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXRdsUpgradeV1Response upgradeRdsByName(@ResourceName String name, DistroXRdsUpgradeV1Request distroxRdsUpgradeRequest) {
        validateClusterName(name);
        return rdsUpgradeService.triggerUpgrade(NameOrCrn.ofName(name), distroxRdsUpgradeRequest);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXRdsUpgradeV1Response upgradeRdsByCrn(@ResourceCrn String crn, DistroXRdsUpgradeV1Request distroxRdsUpgradeRequest) {
        return rdsUpgradeService.triggerUpgrade(NameOrCrn.ofCrn(crn), distroxRdsUpgradeRequest);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXUpgradeV1Response upgradeClusterByName(@ResourceName String clusterName, DistroXUpgradeV1Request distroxUpgradeRequest) {
        validateClusterName(clusterName);
        NameOrCrn nameOrCrn = NameOrCrn.ofName(clusterName);
        Long workspaceId = restRequestThreadLocalService.getRequestedWorkspaceId();
        distroXUpgradeService.validateCodCluster(nameOrCrn, distroxUpgradeRequest.getShowAvailableImages(), workspaceId);
        return distroXUpgradeService.upgradeCluster(distroxUpgradeRequest, nameOrCrn, false, workspaceId);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXUpgradeV1Response upgradeClusterByCrn(@ResourceCrn String clusterCrn, DistroXUpgradeV1Request distroxUpgradeRequest) {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(clusterCrn);
        Long workspaceId = restRequestThreadLocalService.getRequestedWorkspaceId();
        distroXUpgradeService.validateCodCluster(nameOrCrn, distroxUpgradeRequest.getShowAvailableImages(), workspaceId);
        return distroXUpgradeService.upgradeCluster(distroxUpgradeRequest, nameOrCrn, false, workspaceId);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXUpgradeV1Response prepareClusterUpgradeByName(@ResourceName String clusterName, DistroXUpgradeV1Request distroxUpgradeRequest) {
        validateClusterName(clusterName);
        return distroXUpgradeService.upgradeCluster(distroxUpgradeRequest, NameOrCrn.ofName(clusterName), true,
                restRequestThreadLocalService.getRequestedWorkspaceId());
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXUpgradeV1Response prepareClusterUpgradeByCrn(@ResourceCrn String clusterCrn, DistroXUpgradeV1Request distroxUpgradeRequest) {
        return distroXUpgradeService.upgradeCluster(distroxUpgradeRequest, NameOrCrn.ofCrn(clusterCrn), true,
                restRequestThreadLocalService.getRequestedWorkspaceId());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXUpgradeReinitiableV1Response getClusterUpgradeReinitiableByName(@ResourceName String name) {
        validateClusterName(name);
        return distroXUpgradeService.checkClusterUpgradeReinitiable(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXUpgradeReinitiableV1Response getClusterUpgradeReinitiableByCrn(@ResourceCrn String crn) {
        return distroXUpgradeService.checkClusterUpgradeReinitiable(NameOrCrn.ofCrn(crn), restRequestThreadLocalService.getRequestedWorkspaceId());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXUpgradeV1Response reinitiateClusterUpgradeByName(@ResourceName String name) {
        validateClusterName(name);
        return distroXUpgradeService.reinitiateClusterUpgrade(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPGRADE_DATAHUB)
    public DistroXUpgradeV1Response reinitiateClusterUpgradeByCrn(@ResourceCrn String crn) {
        return distroXUpgradeService.reinitiateClusterUpgrade(NameOrCrn.ofCrn(crn), restRequestThreadLocalService.getRequestedWorkspaceId());
    }

    @Override
    @InternalOnly
    public DistroXUpgradeV1Response upgradeClusterByNameInternal(@ResourceName String clusterName, DistroXUpgradeV1Request distroxUpgradeRequest,
            @InitiatorUserCrn String initiatorUserCrn, Boolean rollingUpgradeEnabled) {
        validateClusterName(clusterName);
        NameOrCrn nameOrCrn = NameOrCrn.ofName(clusterName);
        distroxUpgradeRequest.setRollingUpgradeEnabled(
                Boolean.TRUE.equals(rollingUpgradeEnabled) || Boolean.TRUE.equals(distroxUpgradeRequest.getRollingUpgradeEnabled()));
        UpgradeV4Request request = upgradeConverter.convert(distroxUpgradeRequest, true);
        return distroXUpgradeService.upgradeCluster(request, nameOrCrn, false, restRequestThreadLocalService.getRequestedWorkspaceId());
    }

    @Override
    @InternalOnly
    public DistroXUpgradeV1Response upgradeClusterByCrnInternal(@ResourceCrn String clusterCrn, DistroXUpgradeV1Request distroxUpgradeRequest,
            @InitiatorUserCrn String initiatorUserCrn, Boolean rollingUpgradeEnabled) {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(clusterCrn);
        distroxUpgradeRequest.setRollingUpgradeEnabled(
                Boolean.TRUE.equals(rollingUpgradeEnabled) || Boolean.TRUE.equals(distroxUpgradeRequest.getRollingUpgradeEnabled()));
        UpgradeV4Request request = upgradeConverter.convert(distroxUpgradeRequest, true);
        return distroXUpgradeService.upgradeCluster(request, nameOrCrn, false, restRequestThreadLocalService.getRequestedWorkspaceId());
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
        return distroXUpgradeService.triggerOsUpgradeByUpgradeSets(NameOrCrn.ofCrn(crn), workspaceId, orderedOsUpgradeSetRequest.getImageId(),
                orderedOsUpgradeSetRequest.getOrderedOsUpgradeSets());
    }

    @Override
    @CheckPermissionByResourceCrnList(action = AuthorizationResourceAction.DESCRIBE_DATAHUB)
    public List<DistroXDatabaseUpgradeStatus> getDatabaseServerUpgradeRequiredByDatahubCrns(@ResourceCrnList List<String> datahubCrns) {
        return rdsUpgradeStatusService.getUpgradeRequiredByDatahubCrns(datahubCrns);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATAHUB)
    public DistroXDatabaseUpgradeStatus getDatabaseServerUpgradeRequiredByDatahubCrn(@ResourceCrn String datahubCrn) {
        return rdsUpgradeStatusService.getUpgradeRequired(NameOrCrn.ofCrn(datahubCrn));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_DATAHUB)
    public DistroXDatabaseUpgradeStatus getDatabaseServerUpgradeRequiredByDatahubName(@ResourceName String datahubName) {
        return rdsUpgradeStatusService.getUpgradeRequired(NameOrCrn.ofName(datahubName));
    }

    private void validateClusterName(String clusterName) {
        stackService.checkLiveStackExistenceByName(clusterName, ThreadBasedUserCrnProvider.getAccountId(), StackType.WORKLOAD);
    }

}
