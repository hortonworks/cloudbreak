package com.sequenceiq.distrox.v1.distrox;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.conf.LimitConfiguration;
import com.sequenceiq.cloudbreak.domain.projection.StackInstanceCount;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.cluster.ClusterDBValidationService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeAvailabilityService;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradePreconditionService;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class StackUpgradeOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpgradeOperations.class);

    @Inject
    private UserService userService;

    @Inject
    private StackService stackService;

    @Inject
    private UpgradeService upgradeService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private LimitConfiguration limitConfiguration;

    @Inject
    private ClusterUpgradeAvailabilityService clusterUpgradeAvailabilityService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private UpgradePreconditionService upgradePreconditionService;

    @Inject
    private ClusterDBValidationService clusterDBValidationService;

    @Inject
    private StackOperations stackOperations;

    public FlowIdentifier upgradeOs(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        LOGGER.debug("Starting to upgrade OS: " + nameOrCrn);
        return upgradeService.upgradeOs(workspaceId, nameOrCrn);
    }

    public FlowIdentifier upgradeCluster(@NotNull NameOrCrn nameOrCrn, Long workspaceId, String imageId) {
        LOGGER.debug("Starting to upgrade cluster: " + nameOrCrn);
        return upgradeService.upgradeCluster(workspaceId, nameOrCrn, imageId);
    }

    public UpgradeOptionV4Response checkForOsUpgrade(@NotNull NameOrCrn nameOrCrn, CloudbreakUser cloudbreakUser, Long workspaceId) {
        User user = userService.getOrCreate(cloudbreakUser);
        if (nameOrCrn.hasName()) {
            return upgradeService.getOsUpgradeOptionByStackNameOrCrn(workspaceId, nameOrCrn, user);
        } else {
            LOGGER.debug("No stack name provided for upgrade, found: " + nameOrCrn);
            throw new BadRequestException("Please provide a stack name for upgrade");
        }
    }

    public UpgradeV4Response checkForClusterUpgrade(String accountId, @NotNull NameOrCrn nameOrCrn, Long workspaceId, UpgradeV4Request request) {
        return checkForClusterUpgrade(accountId, stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId), workspaceId, request);
    }

    public UpgradeV4Response checkForClusterUpgrade(String accountId, @NotNull Stack stack, Long workspaceId, UpgradeV4Request request) {
        MDCBuilder.buildMdcContext(stack);
        stack.setInstanceGroups(instanceGroupService.getByStackAndFetchTemplates(stack.getId()));
        boolean osUpgrade = upgradeService.isOsUpgrade(request);
        boolean replaceVms = determineReplaceVmsParameter(stack, request.getReplaceVms());
        if (replaceVms) {
            StackInstanceCount stackInstanceCount = instanceMetaDataService.countByStackId(stack.getId());
            Integer upgradeNodeCountLimit = limitConfiguration.getUpgradeNodeCountLimit();
            if (stackInstanceCount.getInstanceCount() > upgradeNodeCountLimit) {
                throw new BadRequestException(
                        String.format("There are %s nodes in the cluster. Upgrade has a limit of %s nodes, above the limit it is unstable. " +
                                "Please downscale the cluster below the limit and retry the upgrade.",
                                stackInstanceCount.getInstanceCount(), upgradeNodeCountLimit));
            }
        }
        UpgradeV4Response upgradeResponse = clusterUpgradeAvailabilityService.checkForUpgradesByName(stack, osUpgrade, replaceVms,
                request.getInternalUpgradeSettings());
        if (CollectionUtils.isNotEmpty(upgradeResponse.getUpgradeCandidates())) {
            clusterUpgradeAvailabilityService.filterUpgradeOptions(accountId, upgradeResponse, request, stack.isDatalake());
        }
        validateDatalakeHasNoRunningDatahub(accountId, workspaceId, stack, upgradeResponse);
        return upgradeResponse;
    }

    private void validateDatalakeHasNoRunningDatahub(String accountId, Long workspaceId, Stack stack, UpgradeV4Response upgradeResponse) {
        if (entitlementService.runtimeUpgradeEnabled(accountId) && StackType.DATALAKE == stack.getType()) {
            LOGGER.info("Checking that the attached DataHubs of the Datalake are in stopped state only in case if Datalake runtime upgarda is enabled" +
                    " in [{}] account on [{}] cluster.", accountId, stack.getName());
            StackViewV4Responses stackViewV4Responses = stackOperations.listByEnvironmentCrn(workspaceId, stack.getEnvironmentCrn(),
                    List.of(StackType.WORKLOAD));
            upgradePreconditionService.checkForRunningAttachedClusters(stackViewV4Responses, upgradeResponse, stack);
        }
    }

    private boolean determineReplaceVmsParameter(Stack stack, Boolean replaceVms) {
        if (stack.isDatalake() || replaceVms != null) {
            return Optional.ofNullable(replaceVms).orElse(Boolean.TRUE);
        } else {
            return upgradePreconditionService.notUsingEphemeralVolume(stack) && clusterDBValidationService.isGatewayRepairEnabled(stack.getCluster());
        }
    }
}
