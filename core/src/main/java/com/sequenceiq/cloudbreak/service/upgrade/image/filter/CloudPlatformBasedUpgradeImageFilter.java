package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import java.util.List;
import java.util.stream.Collectors;

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
        String cloudPlatform = imageFilterParams.getCloudPlatform();
        List<Image> filteredImages = filterImages(imageFilterResult, cloudPlatform);
        LOGGER.debug("After the filtering {} image found with {} cloud platform.", filteredImages.size(), cloudPlatform);
        return new ImageFilterResult(filteredImages, getReason(filteredImages, imageFilterParams));
    }

    @Override
    public String getMessage(ImageFilterParams imageFilterParams) {
        return String.format("There are no eligible images to upgrade for %s cloud platform.", imageFilterParams.getCloudPlatform());
    }

    @Override
    public Integer getFilterOrderNumber() {
        return ORDER_NUMBER;
    }

    private List<Image> filterImages(ImageFilterResult imageFilterResult, String cloudPlatform) {
        return imageFilterResult.getImages()
                .stream()
                .filter(image -> image.getImageSetsByProvider().keySet().stream().anyMatch(key -> key.equalsIgnoreCase(cloudPlatform)))
                .collect(Collectors.toList());
    }

}
