package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.converter.ImageToClouderaManagerRepoConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.model.OsType;

@Service
public class ClusterUpgradePropertiesFactory {

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private StackService stackService;

    @Inject
    private StackImageService stackImageService;

    @Inject
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Inject
    private ImageToClouderaManagerRepoConverter imageToClouderaManagerRepoConverter;

    public ClusterUpgradeProperties create(Long stackId, String targetImageId, boolean lockComponents, boolean rollingUpgradeEnabled, boolean replaceVms)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image currentImage = componentConfigProviderService.getImage(stackId);
        Stack stack = stackService.get(stackId);
        StatedImage targetStatedImage = imageCatalogService
                .getImage(stack.getWorkspace().getId(), currentImage.getImageCatalogUrl(), currentImage.getImageCatalogName(), targetImageId);
        return buildProperties(stack, currentImage, targetStatedImage, lockComponents, rollingUpgradeEnabled, replaceVms);
    }

    private ClusterUpgradeProperties buildProperties(Stack stack, Image currentImage, StatedImage targetStatedImage,
            boolean lockComponents, boolean rollingUpgradeEnabled, boolean replaceVms) {
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image targetCatalogImage = targetStatedImage.getImage();
        Image targetCloudImage = stackImageService.getImageModelFromStatedImage(stack, currentImage, targetStatedImage);

        boolean includePreWarmParcels = !stack.isDatalake();
        Set<ClouderaManagerProduct> allProducts = clouderaManagerProductTransformer.transform(targetCatalogImage, true, includePreWarmParcels);
        ClouderaManagerProduct cdhParcel = allProducts.stream()
                .filter(product -> "CDH".equals(product.getName()))
                .findFirst()
                .orElse(null);
        Set<ClouderaManagerProduct> preWarmParcels = allProducts.stream()
                .filter(product -> cdhParcel == null || !cdhParcel.equals(product))
                .collect(Collectors.toCollection(HashSet::new));

        String runtimeVersion = targetCatalogImage.getPackageVersions()
                .getOrDefault(ImagePackageVersion.STACK.getDisplayName(), targetCatalogImage.getVersion());

        ClusterUpgradeProperties.UpgradeRequestOptions options =
                new ClusterUpgradeProperties.UpgradeRequestOptions(replaceVms, lockComponents, rollingUpgradeEnabled);
        ClusterUpgradeProperties.CurrentImageUpgradeContext currentImageContext = new ClusterUpgradeProperties.CurrentImageUpgradeContext(
                currentImage.getImageId(),
                currentImage.getImageCatalogName(),
                currentImage.getImageCatalogUrl(),
                currentImage.getPackageVersion(ImagePackageVersion.STACK),
                currentImage.getPackageVersions() != null ? new HashMap<>(currentImage.getPackageVersions()) : new HashMap<>(),
                currentImage.getTags() != null ? new HashMap<>(currentImage.getTags()) : new HashMap<>(),
                OsType.getByOsTypeString(currentImage.getOsType()),
                currentImage.getOs(),
                currentImage.getArchitecture(),
                currentImage.getDate(),
                currentImage.getCreated(),
                currentImage.getImageName());
        ClusterUpgradeProperties.TargetImageUpgradeContext targetImageContext = new ClusterUpgradeProperties.TargetImageUpgradeContext(
                targetCatalogImage.getUuid(),
                targetStatedImage.getImageCatalogName(),
                targetStatedImage.getImageCatalogUrl(),
                runtimeVersion,
                targetCatalogImage.getVersion(),
                targetCatalogImage.getPackageVersion(ImagePackageVersion.CDH_BUILD_NUMBER),
                targetCatalogImage.getPackageVersions() != null ? new HashMap<>(targetCatalogImage.getPackageVersions()) : new HashMap<>(),
                targetCatalogImage.getTags() != null ? new HashMap<>(targetCatalogImage.getTags()) : new HashMap<>(),
                OsType.getByOsTypeString(targetCatalogImage.getOsType()),
                targetCatalogImage.getOs(),
                targetCatalogImage.getArchitecture(),
                targetCatalogImage.getDate(),
                targetCatalogImage.getCreated(),
                targetCloudImage.getImageName(),
                targetCatalogImage.getStackDetails(),
                targetCatalogImage.getRepo() != null ? new HashMap<>(targetCatalogImage.getRepo()) : new HashMap<>(),
                targetCatalogImage.getPreWarmParcels(),
                targetCatalogImage.getPreWarmCsd(),
                cdhParcel,
                preWarmParcels,
                imageToClouderaManagerRepoConverter.convert(targetCatalogImage));

        return new ClusterUpgradeProperties(options, currentImageContext, targetImageContext);
    }
}
