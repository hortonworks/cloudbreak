package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@Component
public class CloudPlatformBasedUpgradeImageFilter implements UpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudPlatformBasedUpgradeImageFilter.class);

    private static final int ORDER_NUMBER = 2;

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        List<Image> filteredImages = filterImages(imageFilterResult, image -> image.getImageSetsByProvider().keySet().stream()
                .anyMatch(key -> key.equalsIgnoreCase(imageFilterParams.getImageCatalogPlatform().nameToLowerCase())));
        LOGGER.debug("After the filtering {} image found with {} cloud platform.", filteredImages.size(),
                imageFilterParams.getImageCatalogPlatform().nameToUpperCase());
        return new ImageFilterResult(filteredImages, getReason(filteredImages, imageFilterParams));
    }

    @Override
    public String getMessage(ImageFilterParams imageFilterParams) {
        if (hasTargetImage(imageFilterParams)) {
            return getCantUpgradeToImageMessage(imageFilterParams, String.format("The selected image has no cloud images on %s cloud platform.",
                    imageFilterParams.getImageCatalogPlatform().nameToUpperCase()));
        } else {
            return String.format("There are no eligible images to upgrade for %s cloud platform.",
                    imageFilterParams.getImageCatalogPlatform().nameToUpperCase());
        }
    }

    @Override
    public Integer getFilterOrderNumber() {
        return ORDER_NUMBER;
    }
}
