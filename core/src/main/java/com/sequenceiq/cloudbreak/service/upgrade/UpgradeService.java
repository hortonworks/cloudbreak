package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.flow.api.model.FlowType.NOT_TRIGGERED;
import static java.util.stream.Collectors.toSet;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSet;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.cluster.OSUpgradeByUpgradeSetsService;
import com.sequenceiq.cloudbreak.service.cluster.model.HostGroupName;
import com.sequenceiq.cloudbreak.service.cluster.model.RepairValidation;
import com.sequenceiq.cloudbreak.service.cluster.model.Result;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Component
public class UpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeService.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ImageService imageService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ClusterRepairService clusterRepairService;

    @Inject
    private OSUpgradeByUpgradeSetsService osUpgradeByUpgradeSetsService;

    @Inject
    private DistroXV1Endpoint distroXV1Endpoint;

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private ComponentVersionProvider componentVersionProvider;

    @Inject
    private ClusterBootstrapper clusterBootstrapper;

    @Inject
    private LockedComponentService lockedComponentService;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private EntitlementService entitlementService;

    public UpgradeOptionV4Response getOsUpgradeOptionByStackNameOrCrn(String accountId, NameOrCrn nameOrCrn, User user) {
        StackView stack = stackDtoService.getStackViewByNameOrCrn(nameOrCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        try {
            return getUpgradeOption(accountId, stack, user);
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            LOGGER.warn("Error retrieving image", e);
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    public FlowIdentifier upgradeOsByUpgradeSets(StackView stack, String imageId, List<OrderedOSUpgradeSet> upgradeSets) {
        Image image = findImage(stack);
        ImageChangeDto imageChangeDto = getImageChangeDto(imageId, stack.getId(), image);
        return upgradeOsByUpgradeSets(stack, imageChangeDto, upgradeSets);
    }

    public FlowIdentifier upgradeOsByUpgradeSets(StackView stack, ImageChangeDto imageChangeDto, List<OrderedOSUpgradeSet> upgradeSets) {
        MDCBuilder.buildMdcContext(stack);
        ClusterComponent clusterComponent = clusterBootstrapper.updateSaltComponent(stack);
        try {
            FlowIdentifier flowIdentifier = osUpgradeByUpgradeSetsService.osUpgradeByUpgradeSets(stack, imageChangeDto, upgradeSets);
            if (flowIdentifier != null && NOT_TRIGGERED == flowIdentifier.getType()) {
                LOGGER.warn("Upgrade flow not triggered, reverting salt state upgrade");
                clusterComponentConfigProvider.restorePreviousVersion(clusterComponent);
            }
            return flowIdentifier;
        } catch (RuntimeException e) {
            LOGGER.error("Failed to start OS upgrade by upgrade sets, reverting salt state upgrade", e);
            clusterComponentConfigProvider.restorePreviousVersion(clusterComponent);
            throw e;
        }
    }

    public FlowIdentifier upgradeOs(String accountId, NameOrCrn stackNameOrCrn, boolean keepVariant) {
        StackView stack = stackDtoService.getStackViewByNameOrCrn(stackNameOrCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        ClusterComponent clusterComponent = clusterBootstrapper.updateSaltComponent(stack);
        try {
            FlowIdentifier flowIdentifier = clusterRepairService.repairAll(stack, true, keepVariant);
            if (flowIdentifier != null && NOT_TRIGGERED == flowIdentifier.getType()) {
                LOGGER.warn("Upgrade flow not triggered, reverting salt state upgrade");
                clusterComponentConfigProvider.restorePreviousVersion(clusterComponent);
            }
            return flowIdentifier;
        } catch (RuntimeException e) {
            LOGGER.error("Failed to start OS upgrade, reverting salt state upgrade", e);
            clusterComponentConfigProvider.restorePreviousVersion(clusterComponent);
            throw e;
        }
    }

    public FlowIdentifier upgradeCluster(String accountId, NameOrCrn stackNameOrCrn, String imageId, Boolean rollingUpgradeEnabled) {
        StackView stack = stackDtoService.getStackViewByNameOrCrn(stackNameOrCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        return flowManager.triggerDatalakeClusterUpgrade(stack.getId(), imageId, Boolean.TRUE.equals(rollingUpgradeEnabled));
    }

    public FlowIdentifier prepareClusterUpgrade(String accountId, NameOrCrn stackNameOrCrn, String imageId) {
        StackDto stack = stackDtoService.getByNameOrCrn(stackNameOrCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        if (lockedComponentService.isComponentsLocked(stack, imageId)) {
            throw new BadRequestException("Upgrade preparation is not necessary in case of OS upgrade.");
        }
        Image image = findImage(stack.getStack());
        ImageChangeDto imageChangeDto = getImageChangeDto(imageId, stack.getId(), image);
        return flowManager.triggerClusterUpgradePreparation(stack.getId(), imageChangeDto, false);
    }

    private ImageChangeDto getImageChangeDto(String imageId, Long stackId, Image image) {
        return new ImageChangeDto(stackId, imageId, image.getImageCatalogName(), image.getImageCatalogUrl());
    }

    private Image findImage(StackView stack) {
        try {
            return componentConfigProviderService.getImage(stack.getId());
        } catch (CloudbreakImageNotFoundException e) {
            throw new NotFoundException(String.format("Image not found for stack [%s]", stack.getName()), e);
        }
    }

    public boolean isOsUpgrade(UpgradeV4Request request) {
        return Boolean.TRUE.equals(request.getLockComponents()) && StringUtils.isEmpty(request.getRuntime());
    }

    private UpgradeOptionV4Response getUpgradeOption(String accountId, StackView stack, User user)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image image = findImage(stack);
        UpgradeOptionV4Response upgradeResponse;
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairResult = clusterRepairService.repairWithDryRun(stack.getId());
        if (repairResult.isSuccess()) {
            StatedImage latestImage = getLatestImage(stack.getWorkspaceId(), stack, image, user);
            if (!isLatestImage(stack, image, latestImage)) {
                upgradeResponse = currentImageNotLatest(accountId, stack, image, latestImage);
            } else {
                upgradeResponse = notUpgradable(stack.getWorkspaceId(), image,
                        String.format("According to the image catalog, the current image %s is already the latest version.", image.getImageId()));
            }
        } else {
            upgradeResponse = notUpgradableWithValidationResult(stack.getWorkspaceId(), image, repairResult.getError());
        }
        return upgradeResponse;
    }

    private UpgradeOptionV4Response currentImageNotLatest(String accountId, StackView stack, Image image, StatedImage latestImage)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        UpgradeOptionV4Response upgradeResponse;
        if (entitlementService.isUpgradeAttachedDatahubsCheckSkipped(accountId) || attachedClustersStoppedOrDeleted(stack)) {
            upgradeResponse = upgradeable(image, latestImage, stack);
        } else {
            upgradeResponse = upgradeableAfterAction(image, latestImage, stack, "Please stop connected DataHub clusters before upgrade.");
        }
        return upgradeResponse;
    }

    private boolean isLatestImage(StackView stack, Image currentImage, StatedImage latestImage) {
        String latestImageName = getImageNameForStack(stack, latestImage);
        return Objects.equals(currentImage.getImageName(), latestImageName);
    }

    private String getImageNameForStack(StackView stack, StatedImage statedImage) {
        return statedImage.getImage().getImageSetsByProvider()
                .get(stack.getPlatformVariant().toLowerCase(Locale.ROOT))
                .get(stack.getRegion());
    }

    private boolean attachedClustersStoppedOrDeleted(StackView stack) {
        StackViewV4Responses stackViewV4Responses = distroXV1Endpoint.list(null, stack.getEnvironmentCrn());
        for (StackViewV4Response stackViewV4Response : stackViewV4Responses.getResponses()) {
            if (!Status.getAllowedDataHubStatesForSdxUpgrade().contains(stackViewV4Response.getStatus())
                    || stackViewV4Response.getCluster() != null
                    && !Status.getAllowedDataHubStatesForSdxUpgrade().contains(stackViewV4Response.getCluster().getStatus())) {
                return false;
            }
        }
        return true;
    }

    private StatedImage getLatestImage(Long workspaceId, StackView stack, Image image, User user)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Optional<Blueprint> optionalBlueprint = blueprintService.getByClusterId(stack.getClusterId());
        return imageService
                .determineImageFromCatalog(
                        workspaceId,
                        toImageSettingsRequest(image),
                        stack.getCloudPlatform().toLowerCase(Locale.ROOT),
                        stack.getPlatformVariant(),
                        optionalBlueprint.orElse(null),
                        false,
                        false,
                        user,
                        getImageFilter(image, stack));
    }

    private ImageSettingsV4Request toImageSettingsRequest(Image image) {
        ImageSettingsV4Request imageSettingsV4Request = new ImageSettingsV4Request();
        imageSettingsV4Request.setOs(image.getOs());
        imageSettingsV4Request.setCatalog(image.getImageCatalogName());
        return imageSettingsV4Request;
    }

    private UpgradeOptionV4Response upgradeable(Image image, StatedImage latestImage, StackView stack)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        UpgradeOptionV4Response response = createUpgradeBaseWithCurrent(stack.getWorkspaceId(), image);
        ImageInfoV4Response upgradeImageInfo = new ImageInfoV4Response(
                getImageNameForStack(stack, latestImage),
                latestImage.getImage().getUuid(),
                latestImage.getImageCatalogName(),
                latestImage.getImage().getCreated(),
                latestImage.getImage().getDate(),
                getComponentVersions(latestImage.getImage()));
        response.setUpgrade(upgradeImageInfo);
        LOGGER.info("Datalake upgrade option evaulation finished, image found with image id {}", response.getUpgrade().getImageId());
        return response;
    }

    private UpgradeOptionV4Response upgradeableAfterAction(Image image, StatedImage latestImage, StackView stack, String reason)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        UpgradeOptionV4Response response = notUpgradable(stack.getWorkspaceId(), image, reason);
        ImageInfoV4Response upgradeImageInfo = new ImageInfoV4Response(
                getImageNameForStack(stack, latestImage),
                latestImage.getImage().getUuid(),
                latestImage.getImageCatalogName(),
                latestImage.getImage().getCreated(),
                latestImage.getImage().getDate(),
                getComponentVersions(latestImage.getImage()));
        response.setUpgrade(upgradeImageInfo);
        return response;
    }

    private UpgradeOptionV4Response notUpgradableWithValidationResult(Long workspaceId, Image image, RepairValidation validationResult)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        return notUpgradable(workspaceId, image, validationResult.getValidationErrors().stream().collect(Collectors.joining("; ")));
    }

    private UpgradeOptionV4Response createUpgradeBaseWithCurrent(Long workspaceId, Image image)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image currentImage = imageCatalogService.getImage(
                workspaceId,
                image.getImageCatalogUrl(),
                image.getImageCatalogName(),
                image.getImageId()).getImage();
        ImageInfoV4Response currentImageInfo = new ImageInfoV4Response(
                image.getImageName(),
                image.getImageId(),
                image.getImageCatalogName(),
                currentImage.getCreated(),
                currentImage.getDate(),
                getComponentVersions(currentImage));
        UpgradeOptionV4Response response = new UpgradeOptionV4Response();
        response.setCurrent(currentImageInfo);
        return response;
    }

    private ImageComponentVersions getComponentVersions(com.sequenceiq.cloudbreak.cloud.model.catalog.Image image) {
        return componentVersionProvider.getComponentVersions(image.getPackageVersions(), image.getOs(), image.getDate());
    }

    private UpgradeOptionV4Response notUpgradable(Long workspaceId, Image image, String reason)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        UpgradeOptionV4Response response = createUpgradeBaseWithCurrent(workspaceId, image);
        response.setReason(reason);
        LOGGER.error("Datalake upgrade option evaluation finished with error, reason: {}", response.getReason());
        return response;
    }

    private Predicate<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> getImageFilter(Image image, StackView stack) {
        return packageVersionFilter(image.getPackageVersions()).and(parcelFilter(stack));
    }

    private Predicate<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> packageVersionFilter(Map<String, String> packageVersions) {
        return image -> {
            Map<String, String> catalogPackageVersions = new HashMap<>(image.getPackageVersions());
            catalogPackageVersions.remove(ImagePackageVersion.SALT_BOOTSTRAP.getKey());
            Map<String, String> originalPackageVersions = new HashMap<>(packageVersions);
            originalPackageVersions.remove(ImagePackageVersion.SALT_BOOTSTRAP.getKey());
            return originalPackageVersions.equals(catalogPackageVersions);
        };
    }

    private Predicate<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> parcelFilter(StackView stack) {
        Set<String> originalClusterComponentUrls = clusterComponentConfigProvider.getClouderaManagerProductDetails(stack.getClusterId())
                .stream()
                .map(ClouderaManagerProduct::getParcel)
                .filter(url -> url.endsWith("parcel"))
                .collect(toSet());
        originalClusterComponentUrls.add(clusterComponentConfigProvider.getClouderaManagerRepoDetails(stack.getClusterId()).getBaseUrl());
        return imageFromCatalog -> {
            if (imageFromCatalog.getPreWarmParcels() == null) {
                return false;
            } else {
                Set<String> newClusterComponentUrls = imageFromCatalog.getPreWarmParcels()
                        .stream()
                        .map(parts -> parts.get(1))
                        .collect(toSet());
                newClusterComponentUrls.add(imageFromCatalog.getStackDetails().getRepo().getStack().get(imageFromCatalog.getOsType()));
                newClusterComponentUrls.add(imageFromCatalog.getRepo().get(imageFromCatalog.getOsType()));
                return newClusterComponentUrls.containsAll(originalClusterComponentUrls);
            }
        };
    }
}
