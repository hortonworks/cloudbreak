package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFoundException;
import static java.util.stream.Collectors.toSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.cluster.model.HostGroupName;
import com.sequenceiq.cloudbreak.service.cluster.model.RepairValidation;
import com.sequenceiq.cloudbreak.service.cluster.model.Result;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Component
public class UpgradeService {

    private static final Set<Status> UPGRADEABLE_ATTACHED_DISTRO_X_STATES = Sets.immutableEnumSet(Status.STOPPED, Status.DELETE_COMPLETED,
            Status.CREATE_FAILED, Status.DELETE_FAILED);

    private static final boolean NOT_BASE_IMAGE = false;

    private static final String SALT_BOOTSTRAP = "salt-bootstrap";

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeService.class);

    @Inject
    private StackService stackService;

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
    private DistroXV1Endpoint distroXV1Endpoint;

    @Inject
    private TransactionService transactionService;

    public UpgradeOptionV4Response getUpgradeOptionByStackNameOrCrn(Long workspaceId, NameOrCrn nameOrCrn, User user) {
        try {
            return transactionService.required(() -> {
                Optional<Stack> stack = stackService.findStackByNameOrCrnAndWorkspaceId(nameOrCrn, workspaceId);
                if (stack.isPresent()) {
                    try {
                        return getUpgradeOption(stack.get(), workspaceId, user);
                    } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
                        throw new BadRequestException(e.getMessage());
                    }
                } else {
                    throw notFoundException("Stack", nameOrCrn.toString());
                }
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public FlowIdentifier upgradeByStackName(Long workspaceId, String stackName) {
        try {
            return transactionService.required(() -> {
                Optional<Stack> stack = stackService.findStackByNameAndWorkspaceId(stackName, workspaceId);
                if (stack.isPresent()) {
                    return clusterRepairService.repairAll(stack.get().getId());
                } else {
                    throw notFoundException("Stack", stackName);
                }
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private UpgradeOptionV4Response getUpgradeOption(Stack stack, Long workspaceId, User user)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image image = componentConfigProviderService.getImage(stack.getId());
        UpgradeOptionV4Response upgradeResponse;
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairResult = clusterRepairService.repairWithDryRun(stack.getId());
        if (repairResult.isSuccess()) {
            StatedImage latestImage = getLatestImage(workspaceId, stack, image, user);
            if (!isImageIdTheSame(image, latestImage)) {
                if (attachedClustersStoppedOrDeleted(stack)) {
                    upgradeResponse = upgradeable(image, latestImage, stack);
                } else {
                    upgradeResponse = upgradeableAfterAction(image, latestImage, stack, "Please stop connected DataHub clusters before upgrade.");

                }
            } else {
                upgradeResponse = notUpgradable(image,
                        String.format("According to the image catalog, the current image %s is already the latest version.", image.getImageId()));
            }
        } else {
            upgradeResponse = notUpgradableWithValidationResult(image, repairResult.getError());
        }
        return upgradeResponse;
    }

    private boolean isImageIdTheSame(Image currentImage, StatedImage latestImage) {
        return Objects.equals(currentImage.getImageId(), latestImage.getImage().getUuid());
    }

    private boolean attachedClustersStoppedOrDeleted(Stack stack) {
        StackViewV4Responses stackViewV4Responses = distroXV1Endpoint.list(null, stack.getEnvironmentCrn());
        for (StackViewV4Response stackViewV4Response : stackViewV4Responses.getResponses()) {
            if (!UPGRADEABLE_ATTACHED_DISTRO_X_STATES.contains(stackViewV4Response.getStatus())
                    || (stackViewV4Response.getCluster() != null
                    && !UPGRADEABLE_ATTACHED_DISTRO_X_STATES.contains(stackViewV4Response.getCluster().getStatus()))) {
                return false;
            }
        }
        return true;
    }

    private StatedImage getLatestImage(Long workspaceId, Stack stack, Image image, User user)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        return imageService
                .determineImageFromCatalog(
                        workspaceId,
                        toImageSettingsRequest(image),
                        stack.getCloudPlatform().toLowerCase(),
                        stack.getCluster().getBlueprint(),
                        NOT_BASE_IMAGE,
                        NOT_BASE_IMAGE,
                        user,
                        getImageFilter(image, stack));
    }

    private ImageSettingsV4Request toImageSettingsRequest(Image image) {
        ImageSettingsV4Request imageSettingsV4Request = new ImageSettingsV4Request();
        imageSettingsV4Request.setOs(image.getOs());
        imageSettingsV4Request.setCatalog(image.getImageCatalogName());
        return imageSettingsV4Request;
    }

    private UpgradeOptionV4Response upgradeable(Image image, StatedImage latestImage, Stack stack)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        UpgradeOptionV4Response response = createUpgradeBaseWithCurrent(image);
        ImageInfoV4Response upgradeImageInfo = new ImageInfoV4Response(
                latestImage.getImage().getImageSetsByProvider().get(stack.getPlatformVariant().toLowerCase()).get(stack.getRegion()),
                latestImage.getImage().getUuid(),
                latestImage.getImageCatalogName(),
                latestImage.getImage().getCreated()
        );
        response.setUpgrade(upgradeImageInfo);
        LOGGER.info("Datalake upgrade option evaulation finished, image found with image id {}", response.getUpgrade().getImageId());
        return response;
    }

    private UpgradeOptionV4Response upgradeableAfterAction(Image image, StatedImage latestImage, Stack stack, String reason)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        UpgradeOptionV4Response response = notUpgradable(image, reason);
        ImageInfoV4Response upgradeImageInfo = new ImageInfoV4Response(
                latestImage.getImage().getImageSetsByProvider().get(stack.getPlatformVariant().toLowerCase()).get(stack.getRegion()),
                latestImage.getImage().getUuid(),
                latestImage.getImageCatalogName(),
                latestImage.getImage().getCreated()
        );
        response.setUpgrade(upgradeImageInfo);
        return response;
    }

    private UpgradeOptionV4Response notUpgradableWithValidationResult(Image image, RepairValidation validationResult)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        return notUpgradable(image, validationResult.getValidationErrors().stream().collect(Collectors.joining("; ")));
    }

    private UpgradeOptionV4Response createUpgradeBaseWithCurrent(Image image) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        ImageInfoV4Response currentImageInfo = new ImageInfoV4Response(
                image.getImageName(),
                image.getImageId(),
                image.getImageCatalogName(),
                imageCatalogService.getImage(image.getImageCatalogUrl(), image.getImageCatalogName(), image.getImageId()).getImage().getCreated());
        UpgradeOptionV4Response response = new UpgradeOptionV4Response();
        response.setCurrent(currentImageInfo);
        return response;
    }

    private UpgradeOptionV4Response notUpgradable(Image image, String reason) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        UpgradeOptionV4Response response = createUpgradeBaseWithCurrent(image);
        response.setReason(reason);
        LOGGER.error("Datalake upgrade option evaulation finished with error, reason: {}", response.getReason());
        return response;
    }

    private Predicate<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> getImageFilter(Image image, Stack stack) {
        return packageVersionFilter(image.getPackageVersions()).and(parcelFilter(stack));
    }

    private Predicate<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> packageVersionFilter(Map<String, String> packageVersions) {
        return image -> {
            Map<String, String> catalogPackageVersions = new HashMap<>(image.getPackageVersions());
            catalogPackageVersions.remove(SALT_BOOTSTRAP);
            Map<String, String> originalPackageVersions = new HashMap<>(packageVersions);
            originalPackageVersions.remove(SALT_BOOTSTRAP);
            return originalPackageVersions.equals(catalogPackageVersions);
        };
    }

    private Predicate<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> parcelFilter(Stack stack) {
        Set<String> originalClusterComponentUrls = clusterComponentConfigProvider.getClouderaManagerProductDetails(stack.getCluster().getId())
                .stream()
                .map(ClouderaManagerProduct::getParcel)
                .filter(url -> url.endsWith("parcel"))
                .collect(toSet());
        originalClusterComponentUrls.add(clusterComponentConfigProvider.getClouderaManagerRepoDetails(stack.getCluster().getId()).getBaseUrl());
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
