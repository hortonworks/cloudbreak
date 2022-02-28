package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.cluster.model.HostGroupName;
import com.sequenceiq.cloudbreak.service.cluster.model.RepairValidation;
import com.sequenceiq.cloudbreak.service.cluster.model.Result;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogProvider;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageProvider;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.upgrade.image.ClusterUpgradeImageFilter;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Component
public class ClusterUpgradeAvailabilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeAvailabilityService.class);

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private ImageCatalogProvider imageCatalogProvider;

    @Inject
    private ImageService imageService;

    @Inject
    private ClusterUpgradeImageFilter clusterUpgradeImageFilter;

    @Inject
    private UpgradeOptionsResponseFactory upgradeOptionsResponseFactory;

    @Inject
    private ImageProvider imageProvider;

    @Inject
    private ClusterRepairService clusterRepairService;

    @Inject
    private ImageFilterParamsFactory imageFilterParamsFactory;

    @Inject
    private EntitlementService entitlementService;

    public UpgradeV4Response checkForUpgradesByName(Stack stack, boolean lockComponents, boolean replaceVms, InternalUpgradeSettings internalUpgradeSettings) {
        UpgradeV4Response upgradeOptions = checkForUpgrades(stack, lockComponents, internalUpgradeSettings);
        upgradeOptions.setReplaceVms(replaceVms);
        if (StringUtils.isEmpty(upgradeOptions.getReason())) {
            if (!stack.getStatus().isAvailable()) {
                upgradeOptions.setReason(String.format("Cannot upgrade cluster because it is in %s state.", stack.getStatus()));
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
        return upgradeOptions;
    }

    private boolean shouldValidateForRepair(boolean lockComponents, Boolean replaceVms) {
        return lockComponents || replaceVms == null || replaceVms;
    }

    public void filterUpgradeOptions(String accountId, UpgradeV4Response upgradeOptions, UpgradeV4Request upgradeRequest, boolean datalake) {
        List<ImageInfoV4Response> upgradeCandidates = upgradeOptions.getUpgradeCandidates();
        List<ImageInfoV4Response> filteredUpgradeCandidates;
        // We would like to upgrade to the latest available if no request params exist
        if ((Objects.isNull(upgradeRequest) || upgradeRequest.isEmpty()) && datalake) {
            filteredUpgradeCandidates = filterDatalakeUpgradeCandidates(accountId, upgradeOptions.getCurrent(), upgradeCandidates);
            LOGGER.info("No request param, defaulting to latest image {}", filteredUpgradeCandidates);
        } else {
            String requestImageId = upgradeRequest != null ? upgradeRequest.getImageId() : null;
            String runtime = upgradeRequest != null ? upgradeRequest.getRuntime() : null;

            // Image id param exists
            if (StringUtils.isNotEmpty(requestImageId)) {
                filteredUpgradeCandidates = validateImageId(upgradeCandidates, requestImageId);
                LOGGER.info("Image successfully validated by imageId {}", requestImageId);
                // We would like to upgrade to the latest available image with given runtime
            } else if (StringUtils.isNotEmpty(runtime)) {
                filteredUpgradeCandidates = validateRuntime(upgradeCandidates, runtime);
                LOGGER.info("Image successfully filtered by runtime ({}): {}", runtime, filteredUpgradeCandidates);
            } else {
                filteredUpgradeCandidates = upgradeCandidates;
            }
        }
        upgradeOptions.setUpgradeCandidates(filteredUpgradeCandidates);
    }

    public List<ImageInfoV4Response> filterDatalakeUpgradeCandidates(String accountId, ImageInfoV4Response currentImage,
            List<ImageInfoV4Response> upgradeCandidates) {
        if (entitlementService.runtimeUpgradeEnabled(accountId)) {
            return List.of(upgradeCandidates.stream().max(ImageInfoV4Response.creationBasedComparator()).orElseThrow());
        } else if (currentImage != null) {
            List<ImageInfoV4Response> filteredUpdateCandidates = upgradeCandidates.stream()
                    .filter(candidate -> candidate.getComponentVersions().getCdp().equals(currentImage.getComponentVersions().getCdp()))
                    .collect(Collectors.toList());
            return filteredUpdateCandidates.isEmpty() ? List.of()
                    : List.of(filteredUpdateCandidates.stream().max(ImageInfoV4Response.creationBasedComparator()).orElseThrow());
        }
        return upgradeCandidates;
    }

    private UpgradeV4Response checkForUpgrades(Stack stack, boolean lockComponents, InternalUpgradeSettings internalUpgradeSettings) {
        String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
        UpgradeV4Response upgradeOptions = new UpgradeV4Response();
        try {
            LOGGER.info(String.format("Retrieving images for upgrading stack %s", stack.getName()));
            com.sequenceiq.cloudbreak.cloud.model.Image currentImage = getImage(stack);
            CloudbreakImageCatalogV3 imageCatalog = getImagesFromCatalog(stack.getWorkspace(), currentImage.getImageCatalogName(),
                    currentImage.getImageCatalogUrl());
            Image image = getCurrentImageFromCatalog(currentImage.getImageId(), imageCatalog);
            ImageFilterParams imageFilterParams = createImageFilterParams(image, lockComponents, stack, internalUpgradeSettings);
            ImageFilterResult imageFilterResult = filterImages(accountId, imageCatalog, imageFilterParams);
            LOGGER.info(String.format("%d possible image found for stack upgrade.", imageFilterResult.getImages().size()));
            upgradeOptions = createResponse(image, imageFilterResult, stack.getCloudPlatform(), stack.getRegion(), currentImage.getImageCatalogName());
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException | NotFoundException e) {
            LOGGER.warn("Failed to get images", e);
            upgradeOptions.setReason(String.format("Failed to retrieve image due to %s", e.getMessage()));
        }
        return upgradeOptions;
    }

    private ImageFilterParams createImageFilterParams(Image image, boolean lockComponents, Stack stack, InternalUpgradeSettings internalUpgradeSettings) {
        return imageFilterParamsFactory.create(image, lockComponents, stack, internalUpgradeSettings);
    }

    private Image getCurrentImageFromCatalog(String currentImageId, CloudbreakImageCatalogV3 imageCatalog) throws CloudbreakImageNotFoundException {
        return imageProvider.getCurrentImageFromCatalog(currentImageId, imageCatalog);
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image getImage(Stack stack) throws CloudbreakImageNotFoundException {
        return imageService.getImage(stack.getId());
    }

    private CloudbreakImageCatalogV3 getImagesFromCatalog(Workspace workspace,
            String imageCatalogName, String imageCatalogUrl) throws CloudbreakImageCatalogException {
        try {
            imageCatalogUrl = imageCatalogService.getImageCatalogByName(workspace.getId(), imageCatalogName).getImageCatalogUrl();
            LOGGER.info("Image catalog with name {} and url {} is used for image filtering.", imageCatalogName, imageCatalogUrl);
        } catch (NotFoundException ex) {
            LOGGER.info("Image catalog with name {} not found. The following image catalog url will be used for image filtering: {}.",
                    imageCatalogName, imageCatalogUrl);
        }
        return imageCatalogProvider.getImageCatalogV3(imageCatalogUrl);
    }

    private ImageFilterResult filterImages(String accountId, CloudbreakImageCatalogV3 imageCatalog, ImageFilterParams imageFilterParams) {
        return clusterUpgradeImageFilter.filter(accountId, imageCatalog, imageFilterParams);
    }

    private UpgradeV4Response createResponse(Image currentImage, ImageFilterResult filteredImages, String cloudPlatform, String region,
            String imageCatalogName) {
        return upgradeOptionsResponseFactory.createV4Response(currentImage, filteredImages, cloudPlatform, region, imageCatalogName);
    }

    private List<ImageInfoV4Response> validateRuntime(List<ImageInfoV4Response> upgradeCandidates, String runtime) {
        Supplier<Stream<ImageInfoV4Response>> imagesWithMatchingRuntime = () -> upgradeCandidates.stream().filter(
                imageInfoV4Response -> runtime.equals(imageInfoV4Response.getComponentVersions().getCdp()));
        boolean hasCompatibleImageWithRuntime = imagesWithMatchingRuntime.get().anyMatch(e -> true);
        if (!hasCompatibleImageWithRuntime) {
            String availableRuntimes = upgradeCandidates
                    .stream()
                    .map(ImageInfoV4Response::getComponentVersions)
                    .map(ImageComponentVersions::getCdp)
                    .distinct()
                    .collect(Collectors.joining(","));
            throw new BadRequestException(String.format("There is no image eligible for the cluster upgrade with runtime: %s. "
                    + "Please choose a runtime from the following: %s", runtime, availableRuntimes));
        } else {
            return imagesWithMatchingRuntime.get().collect(Collectors.toList());
        }
    }

    private List<ImageInfoV4Response> validateImageId(List<ImageInfoV4Response> upgradeCandidates, String requestImageId) {
        if (upgradeCandidates.stream()
                .noneMatch(imageInfoV4Response -> imageInfoV4Response.getImageId().equalsIgnoreCase(requestImageId))) {
            String candidates = upgradeCandidates.stream()
                    .map(ImageInfoV4Response::getImageId)
                    .collect(Collectors.joining(","));
            throw new BadRequestException(String.format("The given image (%s) is not eligible for the cluster upgrade. "
                    + "Please choose an id from the following image(s): %s", requestImageId, candidates));
        } else {
            return upgradeCandidates.stream()
                    .filter(imageInfoV4Response -> imageInfoV4Response.getImageId().equalsIgnoreCase(requestImageId))
                    .collect(Collectors.toList());
        }
    }
}
