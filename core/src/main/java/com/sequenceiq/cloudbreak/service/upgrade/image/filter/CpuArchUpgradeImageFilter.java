package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Architecture;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.image.ImageUtil;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@Component
public class CpuArchUpgradeImageFilter implements UpgradeImageFilter {

    private static final int ORDER_NUMBER = 1;

    @Inject
    private ImageUtil imageUtil;

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        Architecture architecture = Architecture.fromStringWithFallback(imageFilterParams.getCurrentImage().getArchitecture());
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
