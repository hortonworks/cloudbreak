package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@Component
public class NonCmUpgradeImageFilter implements UpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NonCmUpgradeImageFilter.class);

    private static final int ORDER_NUMBER = 4;

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        List<Image> filteredImages = filterImages(imageFilterResult, image -> isNotEmpty(image.getPackageVersion(ImagePackageVersion.CM)));
        LOGGER.debug("After the filtering {} image left with Cloudera Manager package.", filteredImages.size());
        return new ImageFilterResult(filteredImages, getReason(filteredImages, imageFilterParams));
    }

    @Override
    public String getMessage(ImageFilterParams imageFilterParams) {
        if (hasTargetImage(imageFilterParams)) {
            return getCantUpgradeToImageMessage(imageFilterParams, "The target image doesn't have Cloudera Manager package installed.");
        } else {
            return "There are no eligible images to upgrade available with Cloudera Manager packages.";
        }
    }

    @Override
    public Integer getFilterOrderNumber() {
        return ORDER_NUMBER;
    }
}
