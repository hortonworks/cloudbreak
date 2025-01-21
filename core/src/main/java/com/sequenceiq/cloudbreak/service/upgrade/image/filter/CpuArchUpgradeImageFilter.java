package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.common.model.Architecture;

@Component
public class CpuArchUpgradeImageFilter implements UpgradeImageFilter {

    private static final int ORDER_NUMBER = 1;

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        Architecture architecture = imageFilterParams.getCurrentImage().getArchitectureEnum();
        List<Image> filteredImages = imageFilterResult.getImages().stream()
                .filter(image -> Architecture.fromStringWithFallback(image.getArchitecture()) == architecture)
                .toList();
        return new ImageFilterResult(filteredImages, getReason(filteredImages, imageFilterParams));
    }

    @Override
    public String getMessage(ImageFilterParams imageFilterParams) {
        if (hasTargetImage(imageFilterParams)) {
            return getCantUpgradeToImageMessage(imageFilterParams, "Can't upgrade to different cpu architecture.");
        } else {
            return "There are no eligible images to upgrade.";
        }
    }

    @Override
    public Integer getFilterOrderNumber() {
        return ORDER_NUMBER;
    }
}
