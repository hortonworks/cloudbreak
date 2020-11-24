package com.sequenceiq.cloudbreak.service.upgrade.image;

import java.util.Map;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.apache.commons.lang3.mutable.Mutable;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradePermissionProvider;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentChecker;

@Component
public class CmAndStackVersionFilter {

    @Inject
    private LockedComponentChecker lockedComponentChecker;

    @Inject
    private UpgradePermissionProvider upgradePermissionProvider;

    public Predicate<Image> filterCmAndStackVersion(ImageFilterParams imageFilterParams, Mutable<String> reason) {
        return candidateImage -> {
            updateReason(imageFilterParams.isLockComponents(), imageFilterParams.getActivatedParcels(), reason);
            return isUpgradePermitted(imageFilterParams, candidateImage);
        };
    }

    private void updateReason(boolean lockComponents, Map<String, String> activatedParcels, Mutable<String> reason) {
        if (lockComponents) {
            reason.setValue("There is at least one activated parcel for which we cannot find image with matching version. "
                    + "Activated parcel(s): " + activatedParcels);
        } else {
            reason.setValue("There is no proper Cloudera Manager or CDP version to upgrade.");
        }
    }

    private boolean isUpgradePermitted(ImageFilterParams imageFilterParams, Image candidateImage) {
        return imageFilterParams.isLockComponents()
                ? lockedComponentChecker.isUpgradePermitted(imageFilterParams.getCurrentImage(), candidateImage, imageFilterParams.getActivatedParcels())
                : isUnlockedCmAndStackUpgradePermitted(imageFilterParams, candidateImage);
    }

    private boolean isUnlockedCmAndStackUpgradePermitted(ImageFilterParams imageFilterParams, Image candidateImage) {
        return upgradePermissionProvider.permitCmUpgrade(imageFilterParams, candidateImage)
                && upgradePermissionProvider.permitStackUpgrade(imageFilterParams, candidateImage);
    }
}
