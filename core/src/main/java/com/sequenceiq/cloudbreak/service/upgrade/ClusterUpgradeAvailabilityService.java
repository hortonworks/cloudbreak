package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.cluster.ClusterDBValidationService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.cluster.model.HostGroupName;
import com.sequenceiq.cloudbreak.service.cluster.model.RepairValidation;
import com.sequenceiq.cloudbreak.service.cluster.model.Result;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.upgrade.image.ClusterUpgradeImageFilter;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@Component
public class ClusterUpgradeAvailabilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeAvailabilityService.class);

    @Inject
    private ClusterUpgradeImageFilter clusterUpgradeImageFilter;

    @Inject
    private UpgradeOptionsResponseFactory upgradeOptionsResponseFactory;

    @Inject
    private ClusterRepairService clusterRepairService;

    @Inject
    private ImageFilterParamsFactory imageFilterParamsFactory;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private CurrentImageRetrieverService currentImageRetrieverService;

    @Inject
    private UpgradePreconditionService upgradePreconditionService;

    @Inject
    private ClusterDBValidationService clusterDBValidationService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private LockedComponentService lockedComponentService;

    public UpgradeV4Response checkForUpgradesByName(Stack stack, boolean lockComponents, Boolean replaceVmsFromRequest,
            InternalUpgradeSettings internalUpgradeSettings, boolean getAllImages, String targetImageId) {
        boolean replaceVms = determineReplaceVmsParameter(stack, replaceVmsFromRequest, lockComponents, true);
        UpgradeV4Response upgradeOptions = checkForUpgrades(stack, lockComponents, replaceVms, internalUpgradeSettings, getAllImages, targetImageId);
        upgradeOptions.setReplaceVms(replaceVms);
        addReasonIfNecessary(stack, lockComponents, replaceVms, upgradeOptions);
        return upgradeOptions;
    }

    public UpgradeV4Response checkForUpgrades(Stack stack, boolean lockComponents, boolean replaceVms, InternalUpgradeSettings internalUpgradeSettings,
            boolean getAllImages, String targetImageId) {
        UpgradeV4Response upgradeOptions = new UpgradeV4Response();
        try {
            LOGGER.info(String.format("Retrieving images for upgrading stack %s", stack.getName()));
            Image currentImage = currentImageRetrieverService.retrieveCurrentModelImage(stack);
            ImageFilterParams imageFilterParams =
                    imageFilterParamsFactory.create(targetImageId, currentImage, lockComponents, replaceVms, stack, internalUpgradeSettings, getAllImages);
            ImageFilterResult imageFilterResult = getAvailableImagesForUpgrade(stack, currentImage.getImageCatalogName(), imageFilterParams);
            LOGGER.info(String.format("%d possible image found for stack upgrade.", imageFilterResult.getImages().size()));
            upgradeOptions = createResponse(imageFilterResult, imageFilterParams);
        } catch (CloudbreakImageNotFoundException | NotFoundException e) {
            LOGGER.warn("Failed to get images", e);
            upgradeOptions.setReason(String.format("Failed to retrieve image due to %s", e.getMessage()));
        }
        return upgradeOptions;
    }

    public boolean determineReplaceVmsParameter(StackDtoDelegate stack, Boolean replaceVmsFromRequest, boolean lockComponents, boolean preCheck) {
        if (stack.getStack().isDatalake()) {
            LOGGER.debug("ReplaceVms is always true for datalakes.");
            return true;
        } else if (!upgradePreconditionService.notUsingEphemeralVolume(stack)) {
            LOGGER.debug("Cluster uses ephemeral volume, replaceVms should be false.");
            return false;
        } else if (!clusterDBValidationService.isGatewayRepairEnabled(stack.getCluster())) {
            LOGGER.debug("Gateway repair is not enabled, replaceVms should be false.");
            return false;
        } else if (replaceVmsFromRequest != null) {
            LOGGER.debug("ReplaceVms is specified in the request: {}", replaceVmsFromRequest);
            if (!preCheck && Boolean.TRUE.equals(replaceVmsFromRequest)) {
                cloudbreakEventService.fireCloudbreakEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_FORCE_OS_UPGRADE_REQUESTED);
            }
            return replaceVmsFromRequest;
        } else if (entitlementService.isDatahubForceOsUpgradeEnabled(Crn.safeFromString(stack.getResourceCrn()).getAccountId())) {
            LOGGER.debug("Force OS upgrade entitlement is enabled, replaceVms should be true.");
            if (!preCheck) {
                cloudbreakEventService.fireCloudbreakEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_FORCE_OS_UPGRADE_ENABLED);
            }
            return true;
        } else if (!lockComponents) {
            LOGGER.info("ReplaceVms parameter has been overridden to false for stack {} in case of distrox runtime upgrade.", stack.getName());
            return false;
        } else {
            LOGGER.debug("Default value for replaceVms param is true");
            return true;
        }
    }

    public boolean determineLockComponentsParam(UpgradeV4Request request, ImageInfoV4Response targetImage, StackDto stack) {
        return request.getLockComponents() != null ? request.getLockComponents() : isComponentsLocked(stack, targetImage);
    }

    private boolean isComponentsLocked(StackDto stack, ImageInfoV4Response targetImage) {
        return lockedComponentService.isComponentsLocked(stack, targetImage.getImageId());
    }

    private void addReasonIfNecessary(Stack stack, boolean lockComponents, Boolean replaceVms, UpgradeV4Response upgradeOptions) {
        if (StringUtils.isEmpty(upgradeOptions.getReason())) {
            if (!stack.getStatus().isAvailable()) {
                upgradeOptions.setReason(String.format("Cannot upgrade cluster because it is in %s state.", stack.getStatus()));
                LOGGER.warn(upgradeOptions.getReason());
            } else if (stack.isDatalake() && instanceMetaDataService.anyInstanceStopped(stack.getId())) {
                upgradeOptions.setReason("Cannot upgrade cluster because there is stopped instance.");
                LOGGER.warn(upgradeOptions.getReason());
            } else if (shouldValidateForRepair(lockComponents, replaceVms)) {
                LOGGER.debug("Validate for repair");
                Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> validationResult = clusterRepairService.repairWithDryRun(stack.getId());
                if (validationResult.isError()) {
                    upgradeOptions.setReason(String.join(",", validationResult.getError().getValidationErrors()));
                    LOGGER.warn(String.format("Cannot upgrade cluster because: %s", upgradeOptions.getReason()));
                }
            }
        }
    }

    private boolean shouldValidateForRepair(boolean lockComponents, Boolean replaceVms) {
        return lockComponents || replaceVms == null || Boolean.TRUE.equals(replaceVms);
    }

    private ImageFilterResult getAvailableImagesForUpgrade(Stack stack, String imageCatalogName, ImageFilterParams imageFilterParams) {
        return clusterUpgradeImageFilter.getAvailableImagesForUpgrade(stack.getWorkspace().getId(), imageCatalogName, imageFilterParams);
    }

    private UpgradeV4Response createResponse(ImageFilterResult filteredImages, ImageFilterParams imageFilterParams) {
        return upgradeOptionsResponseFactory.createV4Response(imageFilterParams.getCurrentImage(), filteredImages, imageFilterParams.getCloudPlatform(),
                imageFilterParams.getRegion(), imageFilterParams.getImageCatalogName());
    }
}
