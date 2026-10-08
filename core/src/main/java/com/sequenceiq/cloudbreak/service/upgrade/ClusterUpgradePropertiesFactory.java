package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.converter.ImageToClouderaManagerRepoConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.model.OsType;

@Service
public class ClusterUpgradePropertiesFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradePropertiesFactory.class);

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

    public ClusterUpgradeProperties create(ImageChangeDto imageChangeDto, boolean lockComponents, boolean rollingUpgradeEnabled, boolean replaceVms) {
        try {
            Image currentImage = componentConfigProviderService.getImage(imageChangeDto.getStackId());
            Stack stack = stackService.get(imageChangeDto.getStackId());
            String catalogUrl = imageChangeDto.getImageCatalogUrl() != null ? imageChangeDto.getImageCatalogUrl() : currentImage.getImageCatalogUrl();
            String catalogName = imageChangeDto.getImageCatalogName() != null ? imageChangeDto.getImageCatalogName() : currentImage.getImageCatalogName();
            StatedImage targetStatedImage = imageCatalogService.getImage(stack.getWorkspace().getId(), catalogUrl, catalogName, imageChangeDto.getImageId());
            return buildProperties(stack, currentImage, targetStatedImage, lockComponents, rollingUpgradeEnabled, replaceVms);
        } catch (CloudbreakImageNotFoundException e) {
            throw new NotFoundException("Image not found for cluster upgrade", e);
        } catch (CloudbreakImageCatalogException e) {
            throw new CloudbreakServiceException("Image catalog is not reachable", e);
        }
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
                .getOrDefault(ImagePackageVersion.STACK.getKey(), targetCatalogImage.getVersion());

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

        LOGGER.debug("Resolved cluster upgrade properties for target image {} with runtime version {}", targetCatalogImage.getUuid(), runtimeVersion);
        return new ClusterUpgradeProperties(options, currentImageContext, targetImageContext);
    }
}
