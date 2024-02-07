package com.sequenceiq.distrox.v1.distrox;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSetRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.cloud.model.component.PreparedImages;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.cluster.ClusterDBValidationService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeAvailabilityService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeCandidateFilterService;
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
    private ClusterUpgradeAvailabilityService clusterUpgradeAvailabilityService;

    @Inject
    private UpgradePreconditionService upgradePreconditionService;

    @Inject
    private ClusterDBValidationService clusterDBValidationService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ClusterUpgradeCandidateFilterService clusterUpgradeCandidateFilterService;

    public FlowIdentifier upgradeOs(@NotNull NameOrCrn nameOrCrn, String accountId, boolean keepVariant) {
        LOGGER.debug("Starting to upgrade OS: " + nameOrCrn);
        return upgradeService.upgradeOs(accountId, nameOrCrn, keepVariant);
    }

    public FlowIdentifier upgradeOsByUpgradeSets(@NotNull NameOrCrn nameOrCrn, Long workspaceId, OrderedOSUpgradeSetRequest orderedOsUpgradeSetRequest) {
        LOGGER.debug("Starting to upgrade OS: " + nameOrCrn);
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        return upgradeService.upgradeOsByUpgradeSets(stack, orderedOsUpgradeSetRequest.getImageId(), orderedOsUpgradeSetRequest.getOrderedOsUpgradeSets());
    }

    public FlowIdentifier upgradeCluster(@NotNull NameOrCrn nameOrCrn, String accountId, String imageId, Boolean rollingUpgradeEnabled) {
        LOGGER.debug("Starting to upgrade cluster: " + nameOrCrn);
        return upgradeService.upgradeCluster(accountId, nameOrCrn, imageId, rollingUpgradeEnabled);
    }

    public FlowIdentifier prepareClusterUpgrade(@NotNull NameOrCrn nameOrCrn, String accountId, String imageId) {
        LOGGER.debug("Starting to prepare upgrade for cluster: " + nameOrCrn);
        return upgradeService.prepareClusterUpgrade(accountId, nameOrCrn, imageId);
    }

    public UpgradeOptionV4Response checkForOsUpgrade(@NotNull NameOrCrn nameOrCrn, CloudbreakUser cloudbreakUser, String accountId) {
        User user = userService.getOrCreate(cloudbreakUser);
        if (nameOrCrn.hasName()) {
            return upgradeService.getOsUpgradeOptionByStackNameOrCrn(accountId, nameOrCrn, user);
        } else {
            LOGGER.debug("No stack name provided for upgrade, found: " + nameOrCrn);
            throw new BadRequestException("Please provide a stack name for upgrade");
        }
    }

    public UpgradeV4Response checkForClusterUpgrade(String accountId, @NotNull NameOrCrn nameOrCrn, Long workspaceId, UpgradeV4Request request) {
        return checkForClusterUpgrade(accountId, stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId), request);
    }

    public UpgradeV4Response checkForClusterUpgrade(String accountId, @NotNull Stack stack, UpgradeV4Request request) {
        MDCBuilder.buildMdcContext(stack);
        stack.setInstanceGroups(instanceGroupService.getByStackAndFetchTemplates(stack.getId()));
        boolean osUpgrade = upgradeService.isOsUpgrade(request);
        boolean replaceVms = determineReplaceVmsParameter(stack, request.getReplaceVms());
        boolean getAllImages = request.getImageId() != null;
        UpgradeV4Response upgradeResponse = clusterUpgradeAvailabilityService.checkForUpgradesByName(stack, osUpgrade, replaceVms,
                request.getInternalUpgradeSettings(), getAllImages, request.getImageId());
        if (CollectionUtils.isNotEmpty(upgradeResponse.getUpgradeCandidates())) {
            clusterUpgradeCandidateFilterService.filterUpgradeOptions(upgradeResponse, request, stack.isDatalake());
            populateCandidatesWithPreparedFlag(stack, upgradeResponse);
        }
        validateAttachedDataHubsForDataLake(accountId, stack, upgradeResponse, request);
        LOGGER.debug("Upgrade response after validations: {}", upgradeResponse);
        return upgradeResponse;
    }

    private void populateCandidatesWithPreparedFlag(Stack stack, UpgradeV4Response upgradeResponse) {
        List<String> preparedImages = getPreparedImagesList(stack.getId());
        upgradeResponse.getUpgradeCandidates().forEach(imageInfoV4Response -> {
            if (!ObjectUtils.isEmpty(preparedImages) && preparedImages.contains(imageInfoV4Response.getImageId())) {
                imageInfoV4Response.setPrepared(true);
            }
        });
    }

    private void validateAttachedDataHubsForDataLake(String accountId, Stack stack, UpgradeV4Response upgradeResponse, UpgradeV4Request request) {
        InternalUpgradeSettings internalUpgradeSettings = request.getInternalUpgradeSettings();
        if (StackType.DATALAKE == stack.getType()
                && (internalUpgradeSettings == null || !internalUpgradeSettings.isUpgradePreparation())) {
            LOGGER.info("Checking that the attached DataHubs of the Data lake are both in stopped state and upgradeable only in case if "
                    + "Data lake runtime upgrade is enabled in [{}] account on [{}] cluster.", accountId, stack.getName());
            List<? extends StackDtoDelegate> datahubsInEnvironment = stackDtoService.findAllByEnvironmentCrnAndStackType(stack.getEnvironmentCrn(),
                    List.of(StackType.WORKLOAD));
            boolean rollingUpgradeEnabled = isRollingUpgradeEnabled(request);
            upgradeResponse.appendReason(upgradePreconditionService.checkForRunningAttachedClusters(datahubsInEnvironment, request.
                    isSkipDataHubValidation(), rollingUpgradeEnabled, accountId));
        }
    }

    private boolean isRollingUpgradeEnabled(UpgradeV4Request request) {
        return Optional.ofNullable(request.getInternalUpgradeSettings()).map(InternalUpgradeSettings::isRollingUpgradeEnabled).orElse(false);
    }

    private boolean determineReplaceVmsParameter(Stack stack, Boolean replaceVms) {
        if (stack.isDatalake() || replaceVms != null) {
            return Optional.ofNullable(replaceVms).orElse(Boolean.TRUE);
        } else {
            return upgradePreconditionService.notUsingEphemeralVolume(stack) && clusterDBValidationService.isGatewayRepairEnabled(stack.getCluster());
        }
    }

    private List<String> getPreparedImagesList(long stackId) {
        List<String> preparedImages = Lists.newArrayList();
        Optional<Long> clusterId = clusterService.findClusterIdByStackId(stackId);
        if (clusterId.isPresent()) {
            ClusterComponent clusterComponent = clusterComponentConfigProvider.getComponent(clusterId.get(), ComponentType.CLUSTER_UPGRADE_PREPARED_IMAGES,
                    ComponentType.CLUSTER_UPGRADE_PREPARED_IMAGES.name());
            try {
                if (!ObjectUtils.isEmpty(clusterComponent)) {
                    preparedImages = clusterComponent.getAttributes().get(PreparedImages.class).getPreparedImages();
                }
            } catch (IOException ex) {
                LOGGER.error("Unable to read prepared list of images from Cluster Component for stack - {}", stackId);
            }
        }
        return preparedImages;
    }
}
