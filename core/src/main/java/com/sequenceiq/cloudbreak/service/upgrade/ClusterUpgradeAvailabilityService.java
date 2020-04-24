package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.Comparator;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV2;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.cluster.model.HostGroupName;
import com.sequenceiq.cloudbreak.service.cluster.model.RepairValidation;
import com.sequenceiq.cloudbreak.service.cluster.model.Result;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogProvider;
import com.sequenceiq.cloudbreak.service.image.ImageProvider;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class ClusterUpgradeAvailabilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeAvailabilityService.class);

    @Inject
    private StackService stackService;

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
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    public UpgradeV4Response checkForUpgradesByName(Long workspaceId, String stackName, boolean lockComponents) {
        UpgradeV4Response upgradeOptions = new UpgradeV4Response();
        Stack stack = stackService.getByNameInWorkspace(stackName, workspaceId);
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> validationResult = clusterRepairService.repairWithDryRun(stack.getId());
        if (!stack.getStatus().isAvailable()) {
            upgradeOptions.setReason(String.format("Cannot upgrade cluster because it is in %s state.", stack.getStatus()));
            LOGGER.warn(upgradeOptions.getReason());
        } else if (validationResult.isError()) {
            upgradeOptions.setReason(String.join(",", validationResult.getError().getValidationErrors()));
            LOGGER.warn(String.format("Cannot upgrade cluster because: %s", upgradeOptions.getReason()));
        } else {
            upgradeOptions = checkForUpgrades(stack, lockComponents);
        }
        return upgradeOptions;
    }

    public UpgradeV4Response checkForNotAttachedClusters(StackViewV4Responses stackViewV4Responses, UpgradeV4Response upgradeOptions) {

        String notStoppedAttachedClusters = stackViewV4Responses.getResponses().stream()
                .filter(stackViewV4Response -> !Status.getAllowedDataHubStatesForSdxUpgrade().contains(stackViewV4Response.getStatus())
                        || (stackViewV4Response.getCluster() != null
                        && !Status.getAllowedDataHubStatesForSdxUpgrade().contains(stackViewV4Response.getCluster().getStatus())))
                .map(StackViewV4Response::getName).collect(Collectors.joining(","));
        if (!notStoppedAttachedClusters.isEmpty()) {
            upgradeOptions.setReason(String.format("There are attached Data Hub clusters in incorrect state: %s. "
                    + "Please stop those to be able to perform the upgrade.", notStoppedAttachedClusters));
        }
        return upgradeOptions;
    }

    public UpgradeV4Response checkIfClusterUpgradable(Long workspaceId, String stackName, UpgradeV4Response upgradeOptions) {

        Stack stack = stackService.getByNameInWorkspaceWithLists(stackName, workspaceId).orElseThrow();
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfigWithoutLists(stack);

            try {
                hostOrchestrator.checkIfClusterUpgradable(primaryGatewayConfig);
            } catch (CloudbreakOrchestratorFailedException e) {
                upgradeOptions.appendReason(e.getMessage());
            }
            return upgradeOptions;
    }

    public void filterUpgradeOptions(UpgradeV4Response upgradeOptions, UpgradeV4Request upgradeRequest) {
        List<ImageInfoV4Response> upgradeCandidates = upgradeOptions.getUpgradeCandidates();
        List<ImageInfoV4Response> filteredUpgradeCandidates;
        // We would like to upgrade to latest available if no request params exist
        if (Objects.isNull(upgradeRequest) || upgradeRequest.isEmpty()) {
            filteredUpgradeCandidates = List.of(upgradeCandidates.stream().max(getComparator()).orElseThrow());
            LOGGER.info("No request param, defaulting to latest image {}", filteredUpgradeCandidates);
        } else {
            String requestImageId = upgradeRequest.getImageId();
            String runtime = upgradeRequest.getRuntime();
            boolean lockComponents = Boolean.TRUE.equals(upgradeRequest.getLockComponents());

            // Image id param exists
            if (StringUtils.isNotEmpty(requestImageId)) {
                filteredUpgradeCandidates = validateImageId(upgradeCandidates, requestImageId);
                LOGGER.info("Image successfully validated by imageId {}", requestImageId);
            // We would like to upgrade to latest available image with given runtime
            } else if (StringUtils.isNotEmpty(runtime)) {
                filteredUpgradeCandidates = validateRuntime(upgradeCandidates, runtime);
                LOGGER.info("Image successfully filtered by runtime ({}): {}", runtime, filteredUpgradeCandidates);
            } else if (lockComponents) {
                filteredUpgradeCandidates = List.of(upgradeCandidates.stream().max(getComparator()).orElseThrow());
            } else {
                filteredUpgradeCandidates = upgradeCandidates;
            }
        }
        upgradeOptions.setUpgradeCandidates(filteredUpgradeCandidates);
    }

    private UpgradeV4Response checkForUpgrades(Stack stack, boolean lockComponents) {
        UpgradeV4Response upgradeOptions = new UpgradeV4Response();
        try {
            LOGGER.info(String.format("Retrieving images for upgrading stack %s", stack.getName()));
            com.sequenceiq.cloudbreak.cloud.model.Image currentImage = getImage(stack);
            CloudbreakImageCatalogV2 imageCatalog = getImagesFromCatalog(currentImage.getImageCatalogUrl());
            Image image = getCurrentImageFromCatalog(currentImage.getImageId(), imageCatalog);
            ImageFilterResult filteredImages = filterImages(imageCatalog, image, stack.cloudPlatform(), lockComponents);
            LOGGER.info(String.format("%d possible image found for stack upgrade.", filteredImages.getAvailableImages().getCdhImages().size()));
            upgradeOptions = createResponse(image, filteredImages, stack.getCloudPlatform(), stack.getRegion(), currentImage.getImageCatalogName());
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException | NotFoundException e) {
            LOGGER.warn("Failed to get images", e);
            upgradeOptions.setReason(String.format("Failed to retrieve imaged due to %s", e.getMessage()));
        }
        return upgradeOptions;
    }

    private Image getCurrentImageFromCatalog(String currentImageId, CloudbreakImageCatalogV2 imageCatalog) throws CloudbreakImageNotFoundException {
        return imageProvider.getCurrentImageFromCatalog(currentImageId, imageCatalog);
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image getImage(Stack stack) throws CloudbreakImageNotFoundException {
        return imageService.getImage(stack.getId());
    }

    private CloudbreakImageCatalogV2 getImagesFromCatalog(String imageCatalogUrl) throws CloudbreakImageCatalogException {
        return imageCatalogProvider.getImageCatalogV2(imageCatalogUrl);
    }

    private ImageFilterResult filterImages(CloudbreakImageCatalogV2 imageCatalog, Image currentImage, String cloudPlatform, boolean lockComponents) {
        return clusterUpgradeImageFilter.filter(getCdhImages(imageCatalog), imageCatalog.getVersions(), currentImage, cloudPlatform, lockComponents);
    }

    private List<Image> getCdhImages(CloudbreakImageCatalogV2 imageCatalog) {
        return imageCatalog.getImages().getCdhImages();
    }

    private UpgradeV4Response createResponse(Image currentImage, ImageFilterResult filteredImages, String cloudPlatform, String region,
            String imageCatalogName) {
        return upgradeOptionsResponseFactory.createV4Response(currentImage, filteredImages, cloudPlatform, region, imageCatalogName);
    }

    private List<ImageInfoV4Response> validateRuntime(List<ImageInfoV4Response> upgradeCandidates, String runtime) {
        Supplier<Stream<ImageInfoV4Response>> imagesWithMatchingRuntime = () ->  upgradeCandidates.stream().filter(
                imageInfoV4Response -> runtime.equals(imageInfoV4Response.getComponentVersions().getCdp()));
        boolean hasCompatbileImageWithRuntime = imagesWithMatchingRuntime.get().anyMatch(e -> true);
        if (!hasCompatbileImageWithRuntime) {
            String availableRuntimes = upgradeCandidates
                    .stream()
                    .map(ImageInfoV4Response::getComponentVersions)
                    .map(ImageComponentVersions::getCdp)
                    .distinct()
                    .collect(Collectors.joining(","));
            throw new BadRequestException(String.format("There is no image eligible for upgrading the cluster with runtime: %s. "
                    + "Please choose a runtime from the following image(s): %s", runtime, availableRuntimes));
        } else {
            return imagesWithMatchingRuntime.get().collect(Collectors.toList());
        }
    }

    private List<ImageInfoV4Response> validateImageId(List<ImageInfoV4Response> upgradeCandidates, String requestImageId) {
        if (upgradeCandidates.stream().noneMatch(imageInfoV4Response -> imageInfoV4Response.getImageId().equalsIgnoreCase(requestImageId))) {
            String candidates = upgradeCandidates.stream().map(ImageInfoV4Response::getImageId).collect(Collectors.joining(","));
            throw new BadRequestException(String.format("The given image (%s) is not eligible for upgrading the cluster. "
                    + "Please choose an id from the following image(s): %s", requestImageId, candidates));
        } else {
            return upgradeCandidates.stream().filter(imageInfoV4Response ->
                    imageInfoV4Response.getImageId().equalsIgnoreCase(requestImageId)).collect(Collectors.toList());
        }
    }

    private Comparator<ImageInfoV4Response> getComparator() {
        return Comparator.comparing(ImageInfoV4Response::getCreated);
    }
}
