package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.cloudbreak.service.upgrade.image.filter.CentOSToRedHatUpgradeImageFilter.isCentOSToRedhatUpgrade;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.upgrade.ImageFilterParamsFactory;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentChecker;

@Service
public class CentOSToRedHatUpgradeAvailabilityService {

    @Inject
    private LockedComponentChecker lockedComponentChecker;

    @Inject
    private ImageFilterParamsFactory imageFilterParamsFactory;

    public boolean isOsUpgradePermitted(com.sequenceiq.cloudbreak.cloud.model.Image currentImage, Image image, StackDtoDelegate stack) {
        Map<String, String> stackRelatedParcels = imageFilterParamsFactory.getStackRelatedParcels(stack);
        return isCentOSToRedhatUpgrade(currentImage, image) && containsSameComponentVersions(image, getCmBuildNumber(currentImage), stackRelatedParcels);
    }

    public boolean isOsUpgradePermitted(com.sequenceiq.cloudbreak.cloud.model.Image currentImage, Image image, Map<String, String> stackRelatedParcels) {
        return isCentOSToRedhatUpgrade(currentImage, image) && containsSameComponentVersions(image, getCmBuildNumber(currentImage), stackRelatedParcels);
    }

    private boolean containsSameComponentVersions(Image targetImage, String cmBuildNumber, Map<String, String> stackRelatedParcels) {
        return lockedComponentChecker.isUpgradePermitted(targetImage, stackRelatedParcels, cmBuildNumber);
    }

    private String getCmBuildNumber(com.sequenceiq.cloudbreak.cloud.model.Image currentImage) {
        return currentImage.getPackageVersion(ImagePackageVersion.CM_BUILD_NUMBER);
    }
}
