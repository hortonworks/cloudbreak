package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import java.util.List;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@Component
public class ImageRegionUpgradeImageFilter implements UpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageRegionUpgradeImageFilter.class);

    private static final int ORDER_NUMBER = 4;

    @Inject
    private ImageService imageService;

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        List<Image> filteredImages = filterImages(imageFilterResult, filterByRegion(imageFilterParams.getImageCatalogPlatform(),
                imageFilterParams.getCloudPlatform(), imageFilterParams.getRegion()));
        LOGGER.debug("After the filtering {} image found with '{}' image catalog platform, '{}' cloud platform and '{}' region.", filteredImages.size(),
                imageFilterParams.getImageCatalogPlatform().nameToUpperCase(), imageFilterParams.getCloudPlatform(), imageFilterParams.getRegion());
        return new ImageFilterResult(filteredImages, getReason(filteredImages, imageFilterParams));
    }

    @Override
    public String getMessage(ImageFilterParams imageFilterParams) {
        if (hasTargetImage(imageFilterParams)) {
            return getCantUpgradeToImageMessage(imageFilterParams,
                    String.format("The target image doesn't have a cloud image on %s cloud platform and in %s region.", imageFilterParams.getCloudPlatform(),
                            imageFilterParams.getRegion()));
        } else {
            return String.format("There are no eligible images to upgrade for '%s' image catalog platform, '%s' cloud platform and '%s' region.",
                    imageFilterParams.getImageCatalogPlatform().nameToUpperCase(), imageFilterParams.getCloudPlatform(), imageFilterParams.getRegion());
        }
    }

    @Override
    public Integer getFilterOrderNumber() {
        return ORDER_NUMBER;
    }

    private Predicate<Image> filterByRegion(ImageCatalogPlatform imageCatalogPlatform, String cloudPlatform, String region) {
        return image -> {
            try {
                imageService.determineImageName(cloudPlatform, imageCatalogPlatform, region, image);
                return true;
            } catch (CloudbreakImageNotFoundException e) {
                LOGGER.error("Image '{}' can't be used as an upgrade candidate: {}", image.getUuid(), e.getMessage());
                return false;
            }
        };
    }
}
