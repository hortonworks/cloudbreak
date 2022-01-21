package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.image.CurrentImageUsageCondition;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@Component
public class CurrentImageUpgradeImageFilter implements UpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentImageUpgradeImageFilter.class);

    private static final int ORDER_NUMBER = 1;

    @Inject
    private CurrentImageUsageCondition currentImageUsageCondition;

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        List<Image> filteredImages = filterImages(imageFilterResult, imageFilterParams);
        logNotEligibleImages(imageFilterResult, filteredImages, LOGGER);
        LOGGER.debug("After the filtering {} image left.", filteredImages.size());
        return new ImageFilterResult(filteredImages, getReason(filteredImages, imageFilterParams));
    }

    @Override
    public String getMessage(ImageFilterParams imageFilterParams) {
        return "There are no other images available in the catalog.";
    }

    @Override
    public Integer getFilterOrderNumber() {
        return ORDER_NUMBER;
    }

    private List<Image> filterImages(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        return imageFilterResult.getImages()
                .stream()
                .filter(filterCurrentImage(imageFilterParams))
                .collect(Collectors.toList());
    }

    private Predicate<Image> filterCurrentImage(ImageFilterParams imageFilterParams) {
        return image -> {
            String currentImageId = imageFilterParams.getCurrentImage().getUuid();
            if (!(image.getUuid().equals(currentImageId) && !isCurrentImageUsedOnInstances(imageFilterParams, currentImageId))) {
                return true;
            } else {
                LOGGER.debug("The current image was removed from the upgrade candidates.");
                return false;
            }
        };
    }

    private boolean isCurrentImageUsedOnInstances(ImageFilterParams imageFilterParams, String currentImageId) {
        return currentImageUsageCondition.currentImageUsedOnInstances(imageFilterParams.getStackId(), currentImageId);
    }
}
