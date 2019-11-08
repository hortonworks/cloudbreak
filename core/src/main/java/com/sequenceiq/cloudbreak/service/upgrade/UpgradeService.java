package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFoundException;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
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
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;

@Component
public class UpgradeService {

    private static final Set<Status> UPGRADEABLE_ATTACHED_DISTRO_X_STATES = Sets.immutableEnumSet(Status.STOPPED, Status.DELETE_COMPLETED);

    private static final boolean FORCE_REPAIR = true;

    private static final boolean REMOVE_ONLY = false;

    private static final String SALT_BOOTSTRAP = "salt-bootstrap";

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
    private ClusterService clusterService;

    @Inject
    private DistroXV1Endpoint distroXV1Endpoint;

    @Inject
    private TransactionService transactionService;

    @Inject
    private HostGroupService hostGroupService;

    public UpgradeOptionV4Response getUpgradeOptionByStackName(Long workspaceId, String stackName, User user) {
        try {
            return transactionService.required(() -> {
                Optional<Stack> stack = stackService.findStackByNameAndWorkspaceId(stackName, workspaceId);
                if (stack.isPresent()) {
                    return getUpgradeOption(stack.get(), workspaceId, user);
                } else {
                    throw notFoundException("Stack", stackName);
                }
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public void upgradeByStackName(Long workspaceId, String stackName) {
        try {
            transactionService.required(() -> {
                Optional<Stack> stack = stackService.findStackByNameAndWorkspaceId(stackName, workspaceId);
                if (stack.isPresent()) {
                    List<String> hostGroupNames = hostGroupService.getByCluster(stack.get().getCluster().getId())
                            .stream()
                            .map(HostGroup::getName)
                            .collect(toList());
                    clusterService.repairCluster(stack.get().getId(), hostGroupNames, REMOVE_ONLY, FORCE_REPAIR);
                    return null;
                } else {
                    throw notFoundException("Stack", stackName);
                }
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private UpgradeOptionV4Response getUpgradeOption(Stack stack, Long workspaceId, User user) {
        if (clusterService.repairSupported(stack) && attachedClustersStoppedOrDeleted(stack)) {
            try {
                Blueprint blueprint = stack.getCluster().getBlueprint();
                Image image = componentConfigProviderService.getImage(stack.getId());
                ImageSettingsV4Request imageSettingsV4Request = toImageSettingsRequest(image);
                boolean baseImage = useBaseImage(image);
                StatedImage latestImage = imageService
                        .determineImageFromCatalog(workspaceId, imageSettingsV4Request, stack.getCloudPlatform().toLowerCase(), blueprint, baseImage, user,
                                getImageFilter(image, baseImage, stack));
                ImageInfoV4Response currentImageInfo = new ImageInfoV4Response(
                        image.getImageName(),
                        image.getImageId(),
                        image.getImageCatalogName(),
                        imageCatalogService.getImage(image.getImageCatalogUrl(), image.getImageCatalogName(), image.getImageId()).getImage().getCreated());
                if (!Objects.equals(image.getImageId(), latestImage.getImage().getUuid())) {
                    ImageInfoV4Response upgradeImageInfo = new ImageInfoV4Response(
                            latestImage.getImage().getImageSetsByProvider().get(stack.getPlatformVariant().toLowerCase()).get(stack.getRegion()),
                            latestImage.getImage().getUuid(),
                            latestImage.getImageCatalogName(),
                            latestImage.getImage().getCreated()
                    );
                    return new UpgradeOptionV4Response(currentImageInfo, upgradeImageInfo);
                } else {
                    UpgradeOptionV4Response upgradeOptionV4Response = new UpgradeOptionV4Response();
                    upgradeOptionV4Response.setCurrent(currentImageInfo);
                    return upgradeOptionV4Response;
                }
            } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
                throw new BadRequestException(e.getMessage());
            }
        } else {
            return new UpgradeOptionV4Response();
        }
    }

    private Predicate<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> getImageFilter(Image image, boolean baseImage, Stack stack) {
        if (baseImage) {
            return packageVersionFilter(image.getPackageVersions(), baseImage);
        } else {
            return packageVersionFilter(image.getPackageVersions(), baseImage)
                    .and(parcelFilter(stack));
        }
    }

    private Predicate<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> parcelFilter(Stack stack) {
        Set<String> originalClusterComponentUrls = clusterComponentConfigProvider.getClouderaManagerProductDetails(stack.getCluster().getId())
                .stream()
                .map(ClouderaManagerProduct::getParcel)
                .collect(toSet());
        originalClusterComponentUrls.add(clusterComponentConfigProvider.getClouderaManagerRepoDetails(stack.getCluster().getId()).getBaseUrl());
        return imageFromCatalaog -> {
            if (imageFromCatalaog.getPreWarmParcels() == null) {
                return false;
            } else {
                Set<String> newClusterComponentUrls = imageFromCatalaog.getPreWarmParcels()
                        .stream()
                        .map(parts -> parts.get(1))
                        .collect(toSet());
                newClusterComponentUrls.add(imageFromCatalaog.getStackDetails().getRepo().getStack().get(imageFromCatalaog.getOsType()));
                newClusterComponentUrls.add(imageFromCatalaog.getRepo().get(imageFromCatalaog.getOsType()));
                return newClusterComponentUrls
                        .equals(originalClusterComponentUrls);
            }
        };
    }

    private Predicate<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> packageVersionFilter(Map<String, String> packageVersions, boolean baseImage) {
        return image -> {
            Map<String, String> catalogPackageVersions = new HashMap<>(image.getPackageVersions());
            catalogPackageVersions.remove(SALT_BOOTSTRAP);
            Map<String, String> originalPackageVersions = new HashMap<>(packageVersions);
            originalPackageVersions.remove(SALT_BOOTSTRAP);
            if (baseImage) {
                return originalPackageVersions.entrySet().containsAll(catalogPackageVersions.entrySet());
            } else {
                return originalPackageVersions.equals(catalogPackageVersions);
            }
        };
    }

    private boolean useBaseImage(Image image) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        return !imageCatalogService.getImage(image.getImageCatalogUrl(), image.getImageCatalogName(), image.getImageId()).getImage().isPrewarmed();
    }

    private boolean attachedClustersStoppedOrDeleted(Stack stack) {
        StackViewV4Responses stackViewV4Responses = distroXV1Endpoint.list(null, stack.getEnvironmentCrn());
        for (StackViewV4Response stackViewV4Response : stackViewV4Responses.getResponses()) {
            if (!(UPGRADEABLE_ATTACHED_DISTRO_X_STATES.contains(stack.getStatus())
                    || UPGRADEABLE_ATTACHED_DISTRO_X_STATES.contains(getClusterStatus(stackViewV4Response)))) {
                return false;
            }
        }
        return true;
    }

    private Status getClusterStatus(StackViewV4Response stack) {
        if (stack.getCluster() == null) {
            return null;
        } else {
            return stack.getCluster().getStatus();
        }
    }

    private ImageSettingsV4Request toImageSettingsRequest(Image image) {
        ImageSettingsV4Request imageSettingsV4Request = new ImageSettingsV4Request();
        imageSettingsV4Request.setOs(image.getOs());
        imageSettingsV4Request.setCatalog(image.getImageCatalogName());
        return imageSettingsV4Request;
    }
}
