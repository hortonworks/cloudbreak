package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@Service
public class ImageRegionBasedUpgradeImageFilter implements UpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageRegionBasedUpgradeImageFilter.class);

    private static final int ORDER_NUMBER = 3;

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        String region = imageFilterParams.getRegion();
        List<Image> filteredImages = region == null ? imageFilterResult.getImages() : filterImages(imageFilterResult, region);
        LOGGER.debug("After the filtering {} image found with {} region.", filteredImages.size(), region);
        return new ImageFilterResult(filteredImages, getReason(filteredImages, imageFilterParams));
    }

    @Override
    public String getMessage(ImageFilterParams imageFilterParams) {
        return String.format("There are no eligible images to upgrade for %s region.", imageFilterParams.getRegion());
    }

    @Override
    public Integer getFilterOrderNumber() {
        return ORDER_NUMBER;
    }

    private List<Image> filterImages(ImageFilterResult imageFilterResult, String region) {
        return imageFilterResult.getImages()
                .stream()
                .filter(image -> image.getImageSetsByProvider().values().stream().anyMatch(value -> value.containsKey(region)))
                .collect(Collectors.toList());
    }
}
