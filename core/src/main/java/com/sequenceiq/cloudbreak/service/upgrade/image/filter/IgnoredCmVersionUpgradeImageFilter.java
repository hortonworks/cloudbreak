package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@Component
public class IgnoredCmVersionUpgradeImageFilter implements UpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(IgnoredCmVersionUpgradeImageFilter.class);

    private static final String IGNORED_CM_VERSION = "7.x.0";

    private static final int ORDER_NUMBER = 5;

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        List<Image> filteredImages = filterImages(imageFilterResult, image -> !image.getPackageVersion(ImagePackageVersion.CM).contains(IGNORED_CM_VERSION));
        logNotEligibleImages(imageFilterResult, filteredImages, LOGGER);
        LOGGER.debug("After the filtering {} image left with proper Cloudera Manager version format.", filteredImages.size());
        return new ImageFilterResult(filteredImages, getReason(filteredImages, imageFilterParams));
    }

    @Override
    public String getMessage(ImageFilterParams imageFilterParams) {
        if (hasTargetImage(imageFilterParams)) {
            return getCantUpgradeToImageMessage(imageFilterParams,
                    String.format("Cloudera Manager version on the target image contains '%s'.", IGNORED_CM_VERSION));
        } else {
            return "There are no eligible images with supported Cloudera Manager or CDP version.";
        }
    }

    @Override
    public Integer getFilterOrderNumber() {
        return ORDER_NUMBER;
    }
}
