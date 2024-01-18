package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static com.sequenceiq.cloudbreak.service.upgrade.image.filter.CentOSToRedHatUpgradeImageFilter.isCentOSToRedHatUpgradableVersion;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@Component
public class OsVersionBasedUpgradeImageFilter implements UpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OsVersionBasedUpgradeImageFilter.class);

    private static final int ORDER_NUMBER = 8;

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        String currentOs = imageFilterParams.getCurrentImage().getOs();
        String currentOsType = imageFilterParams.getCurrentImage().getOsType();
        List<Image> filteredImages = filterImages(imageFilterParams, imageFilterResult);
        LOGGER.debug("After the filtering {} image left with proper OS {} and OS type {}.", filteredImages.size(), currentOs, currentOsType);
        return new ImageFilterResult(filteredImages, getReason(filteredImages, imageFilterParams));
    }

    @Override
    public String getMessage(ImageFilterParams imageFilterParams) {
        return "There are no eligible images to upgrade with the same OS version.";
    }

    @Override
    public Integer getFilterOrderNumber() {
        return ORDER_NUMBER;
    }

    private List<Image> filterImages(ImageFilterParams imageFilterParams, ImageFilterResult imageFilterResult) {
        return imageFilterResult.getImages()
                .stream()
                .filter(image -> isOsVersionsMatch(imageFilterParams.getCurrentImage().getOs(), imageFilterParams.getCurrentImage().getOsType(), image)
                        || isCentOSToRedHatUpgradableVersion(imageFilterParams.getCurrentImage(), image))
                .collect(Collectors.toList());
    }

    private boolean isOsVersionsMatch(String currentOs, String currentOsType, Image newImage) {
        return newImage.getOs().equalsIgnoreCase(currentOs) && newImage.getOsType().equalsIgnoreCase(currentOsType);
    }
}
