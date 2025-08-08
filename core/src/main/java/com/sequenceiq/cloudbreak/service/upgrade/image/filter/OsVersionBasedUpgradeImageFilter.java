package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static com.sequenceiq.common.model.OsType.RHEL9;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.image.CentosToRedHatUpgradeCondition;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@Component
public class OsVersionBasedUpgradeImageFilter implements UpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OsVersionBasedUpgradeImageFilter.class);

    private static final int ORDER_NUMBER = 8;

    @Inject
    private CentosToRedHatUpgradeCondition centosToRedHatUpgradeCondition;

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        String currentOs = imageFilterParams.getCurrentImage().getOs();
        String currentOsType = imageFilterParams.getCurrentImage().getOsType();
        List<Image> filteredImages = filterImages(imageFilterResult,
                image -> notRedhat9Image(image)
                        && (isOsVersionsMatch(imageFilterParams.getCurrentImage(), image) || isCentosToRedhatUpgrade(imageFilterParams, image)));
        LOGGER.debug("After the filtering {} image left with proper OS {} and OS type {}.", filteredImages.size(), currentOs, currentOsType);
        return new ImageFilterResult(filteredImages, getReason(filteredImages, imageFilterParams));
    }

    @Override
    public String getMessage(ImageFilterParams imageFilterParams) {
        if (hasTargetImage(imageFilterParams)) {
            return getCantUpgradeToImageMessage(imageFilterParams, "Can't upgrade to the selected image because it's os type is incompatible.");
        } else {
            return "There are no eligible images to upgrade with the same OS version.";
        }
    }

    @Override
    public Integer getFilterOrderNumber() {
        return ORDER_NUMBER;
    }

    private boolean notRedhat9Image(Image image) {
        return !RHEL9.getOs().equalsIgnoreCase(image.getOs()) && !RHEL9.getOsType().equalsIgnoreCase(image.getOsType());
    }

    private boolean isOsVersionsMatch(com.sequenceiq.cloudbreak.cloud.model.Image currentImage, Image newImage) {
        return newImage.getOs().equalsIgnoreCase(currentImage.getOs()) && newImage.getOsType().equalsIgnoreCase(currentImage.getOsType());
    }

    private boolean isCentosToRedhatUpgrade(ImageFilterParams imageFilterParams, Image image) {
        return centosToRedHatUpgradeCondition.isCentosToRedhatUpgrade(imageFilterParams.getStackId(), image);
    }
}