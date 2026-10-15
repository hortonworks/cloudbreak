package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.image.ImageFilter;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.upgrade.image.filter.ImageRegionUpgradeImageFilter;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@Service
public class BaseImageUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseImageUpgradeService.class);

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private PlatformStringTransformer platformStringTransformer;

    @Inject
    private ImageRegionUpgradeImageFilter imageRegionUpgradeImageFilter;

    @Inject
    private BaseImagePackageVersionsEnricher baseImagePackageVersionsEnricher;

    @Inject
    private ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    @Inject
    private UpgradePermissionProvider upgradePermissionProvider;

    public ImageChangeDto createImageChangeDto(StackDtoDelegate stack, UpgradeComponents components) {
        try {
            Image currentImage = getCurrentImageAndValidateComponents(stack, components);
            String cloudPlatform = stack.getStack().getCloudPlatform();
            ImageCatalogPlatform platform = platformStringTransformer.getPlatformStringForImageCatalog(cloudPlatform, stack.getStack().getPlatformVariant());
            ImageFilter filter = ImageFilter.builder()
                    .withImageCatalog(imageCatalogService.getImageCatalogByName(stack.getWorkspaceId(), currentImage.getImageCatalogName()))
                    .withPlatforms(Set.of(platform))
                    .withArchitecture(Architecture.fromStringWithFallback(currentImage.getArchitecture()))
                    .withOperatingSystems(Set.of(currentImage.getOsType()))
                    .withAdditionalPredicate(imageRegionUpgradeImageFilter.filterByRegion(platform, cloudPlatform, stack.getStack().getRegion()))
                    .withBaseImageEnabled(true)
                    .withIncludeUnversionedImages(true)
                    .build();
            StatedImage targetImage = imageCatalogService.getLatestBaseImage(filter);
            LOGGER.info("Selected base image {} for upgrade with explicit components", targetImage.getImage().getUuid());
            return toImageChangeDto(stack, targetImage, components);
        } catch (CloudbreakImageNotFoundException e) {
            throw new NotFoundException("Image not found for base image upgrade", e);
        } catch (CloudbreakImageCatalogException e) {
            throw new CloudbreakServiceException("Image catalog is not reachable", e);
        }
    }

    public ImageChangeDto createImageChangeDto(StackDtoDelegate stack, UpgradeComponents components, String imageId) {
        try {
            Image currentImage = getCurrentImageAndValidateComponents(stack, components);
            StatedImage targetImage = imageCatalogService.getImageByCatalogName(stack.getWorkspaceId(), imageId, currentImage.getImageCatalogName());
            LOGGER.info("Using previously selected base image {} for upgrade with explicit components", imageId);
            return toImageChangeDto(stack, targetImage, components);
        } catch (CloudbreakImageNotFoundException e) {
            throw new NotFoundException("Image not found for base image upgrade", e);
        } catch (CloudbreakImageCatalogException e) {
            throw new CloudbreakServiceException("Image catalog is not reachable", e);
        }
    }

    private Image getCurrentImageAndValidateComponents(StackDtoDelegate stack, UpgradeComponents components) throws CloudbreakImageNotFoundException {
        Image currentImage = componentConfigProviderService.getImage(stack.getId());
        Set<ClouderaManagerProduct> currentProducts = clouderaManagerProductsProvider.findCdhProduct(stack.getClusterComponents())
                .map(Set::of).orElse(Set.of());
        Map<String, String> currentPackages = baseImagePackageVersionsEnricher.enrich(currentImage, stack).getPackageVersions();
        upgradePermissionProvider.validateRequestedComponentVersions(currentPackages, currentProducts, components);
        return currentImage;
    }

    private ImageChangeDto toImageChangeDto(StackDtoDelegate stack, StatedImage targetImage, UpgradeComponents components) {
        return new ImageChangeDto(stack.getId(), targetImage.getImage().getUuid(), targetImage.getImageCatalogName(),
                targetImage.getImageCatalogUrl(), components);
    }
}
