package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.cloudbreak.service.upgrade.image.ClusterUpgradeImageFilter.CM_PACKAGE_KEY;

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

    private static final String STACK_PACKAGE_KEY = "stack";

    private static final String CDH_BUILD_NUMBER_KEY = "cdh-build-number";

    private static final String CM_BUILD_NUMBER_KEY = "cm-build-number";

    @Inject
    private LockedComponentChecker lockedComponentChecker;

    @Inject
    private UpgradePermissionProvider upgradePermissionProvider;

    public Predicate<Image> filterCmAndStackVersion(Image currentImage, boolean lockComponents, Map<String, String> activatedParcels,
            Mutable<String> reason) {
        return candidateImage -> {
            updateReason(lockComponents, activatedParcels, reason);
            return isUpgradePermitted(currentImage, lockComponents, activatedParcels, candidateImage);
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

    private boolean isUpgradePermitted(Image currentImage, boolean lockComponents, Map<String, String> activatedParcels, Image candidateImage) {
        return lockComponents
                ? lockedComponentChecker.isUpgradePermitted(currentImage, candidateImage, activatedParcels)
                : isUnlockedCmAndStackUpgradePermitted(currentImage, candidateImage);
    }

    private boolean isUnlockedCmAndStackUpgradePermitted(Image currentImage, Image candidateImage) {
        return isCmAndStackUpgradePermitted(currentImage, candidateImage, CM_PACKAGE_KEY, CM_BUILD_NUMBER_KEY)
                && isCmAndStackUpgradePermitted(currentImage, candidateImage, STACK_PACKAGE_KEY, CDH_BUILD_NUMBER_KEY);
    }

    private boolean isCmAndStackUpgradePermitted(Image currentImage, Image candidateImage, String versionKey, String buildNumberKey) {
        return upgradePermissionProvider.permitCmAndStackUpgrade(currentImage, candidateImage, versionKey, buildNumberKey);
    }
}
