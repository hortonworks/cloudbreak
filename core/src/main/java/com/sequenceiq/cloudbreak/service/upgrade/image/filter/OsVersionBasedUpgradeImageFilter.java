package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static com.sequenceiq.common.model.OsType.RHEL9;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.image.CurrentImageUsageCondition;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.cloudbreak.service.upgrade.image.OsChangeUpgradeCondition;
import com.sequenceiq.common.model.OsType;

@Component
public class OsVersionBasedUpgradeImageFilter implements UpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OsVersionBasedUpgradeImageFilter.class);

    private static final int ORDER_NUMBER = 8;

    @Inject
    private OsChangeUpgradeCondition osChangeUpgradeCondition;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private CurrentImageUsageCondition currentImageUsageCondition;

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        String currentOs = imageFilterParams.getCurrentImage().getOs();
        String currentOsType = imageFilterParams.getCurrentImage().getOsType();
        boolean rhel9Enabled = entitlementService.isEntitledToUseOS(ThreadBasedUserCrnProvider.getAccountId(), RHEL9);
        Set<OsType> osUsedByInstances = currentImageUsageCondition.getOSUsedByInstances(imageFilterParams.getStackId());
        List<Image> filteredImages = filterImages(imageFilterResult,
                image -> isSameOsOrAllowedOsChange(imageFilterParams, image, rhel9Enabled, osUsedByInstances));
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

    private boolean isSameOsOrAllowedOsChange(ImageFilterParams imageFilterParams, Image image, boolean rhel9Enabled, Set<OsType> osUsedByInstances) {
        return isOsEntitled(image, rhel9Enabled) &&
                (isOsMatches(imageFilterParams.getCurrentImage(), image) || osChangeUpgradeCondition.isNextMajorOsImage(osUsedByInstances, image));
    }

    private boolean isOsEntitled(Image image, boolean rhel9Enabled) {
        return !RHEL9.matches(image.getOs(), image.getOsType()) || rhel9Enabled;
    }

    private boolean isOsMatches(com.sequenceiq.cloudbreak.cloud.model.Image currentImage, Image newImage) {
        return newImage.getOs().equalsIgnoreCase(currentImage.getOs()) && newImage.getOsType().equalsIgnoreCase(currentImage.getOsType());
    }
}