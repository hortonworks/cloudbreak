package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.cluster.model.HostGroupName;
import com.sequenceiq.cloudbreak.service.cluster.model.RepairValidation;
import com.sequenceiq.cloudbreak.service.cluster.model.Result;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.upgrade.image.ClusterUpgradeImageFilter;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

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

    public UpgradeV4Response checkForUpgradesByName(Stack stack, boolean lockComponents, boolean replaceVms, InternalUpgradeSettings internalUpgradeSettings,
            boolean getAllImages, String targetImageId) {
        UpgradeV4Response upgradeOptions = checkForUpgrades(stack, lockComponents, internalUpgradeSettings, getAllImages, targetImageId);
        upgradeOptions.setReplaceVms(replaceVms);
        addReasonIfNecessary(stack, lockComponents, replaceVms, upgradeOptions);
        return upgradeOptions;
    }

    public UpgradeV4Response checkForUpgrades(Stack stack, boolean lockComponents, InternalUpgradeSettings internalUpgradeSettings, boolean getAllImages,
            String targetImageId) {
        UpgradeV4Response upgradeOptions = new UpgradeV4Response();
        try {
            LOGGER.info(String.format("Retrieving images for upgrading stack %s", stack.getName()));
            Image currentImage = currentImageRetrieverService.retrieveCurrentModelImage(stack);
            ImageFilterParams imageFilterParams =
                    imageFilterParamsFactory.create(targetImageId, currentImage, lockComponents, stack, internalUpgradeSettings, getAllImages);
            ImageFilterResult imageFilterResult = getAvailableImagesForUpgrade(stack, currentImage.getImageCatalogName(), imageFilterParams);
            LOGGER.info(String.format("%d possible image found for stack upgrade.", imageFilterResult.getImages().size()));
            upgradeOptions = createResponse(imageFilterResult, imageFilterParams);
        } catch (CloudbreakImageNotFoundException | NotFoundException e) {
            LOGGER.warn("Failed to get images", e);
            upgradeOptions.setReason(String.format("Failed to retrieve image due to %s", e.getMessage()));
        }
        return upgradeOptions;
    }

    private void addReasonIfNecessary(Stack stack, boolean lockComponents, boolean replaceVms, UpgradeV4Response upgradeOptions) {
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
        return lockComponents || replaceVms == null || replaceVms;
    }

    private ImageFilterResult getAvailableImagesForUpgrade(Stack stack, String imageCatalogName, ImageFilterParams imageFilterParams) {
        return clusterUpgradeImageFilter.getAvailableImagesForUpgrade(stack.getWorkspace().getId(), imageCatalogName, imageFilterParams);
    }

    private UpgradeV4Response createResponse(ImageFilterResult filteredImages, ImageFilterParams imageFilterParams) {
        return upgradeOptionsResponseFactory.createV4Response(imageFilterParams.getCurrentImage(), filteredImages, imageFilterParams.getCloudPlatform(),
                imageFilterParams.getRegion(), imageFilterParams.getImageCatalogName());
    }
}
