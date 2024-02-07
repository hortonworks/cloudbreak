package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@Component
public class TargetImageIdImageFilter implements UpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TargetImageIdImageFilter.class);

    private static final int ORDER_NUMBER = 0;

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        if (hasTargetImage(imageFilterParams)) {
            LOGGER.debug("Target image found with id: {}, filtering out the other images.", imageFilterParams.getTargetImageId());
            List<Image> filteredImages = filterImages(imageFilterResult, image -> Objects.equals(image.getUuid(), imageFilterParams.getTargetImageId()));
            return new ImageFilterResult(filteredImages, getReason(filteredImages, imageFilterParams));
        } else {
            return imageFilterResult;
        }
    }

    @Override
    public String getMessage(ImageFilterParams imageFilterParams) {
        return String.format("Can't upgrade to '%s' image because there is no image with this id in the image catalog.", imageFilterParams.getTargetImageId());
    }

    @Override
    public Integer getFilterOrderNumber() {
        return ORDER_NUMBER;
    }
}
