package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.cloud.PrefixMatchLength.MAJOR;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.cloudera.cdp.shaded.com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.VersionPrefix;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@Service
public class UpgradePermissionProvider {

    @Inject
    private ComponentBuildNumberComparator componentBuildNumberComparator;

    public boolean permitCmAndStackUpgrade(Image currentImage, Image image, String versionKey, String buildNumberKey) {
        String currentVersion = getVersionFromImage(currentImage, versionKey);
        String newVersion = getVersionFromImage(image, versionKey);
        return versionsArePresent(currentVersion, newVersion) && permitCmAndStackUpgradeByComponentVersion(currentVersion, newVersion)
                || permitCmAndStackUpgradeByBuildNumber(currentImage, image, buildNumberKey, currentVersion, newVersion);
    }

    private String getVersionFromImage(Image image, String key) {
        return Optional.ofNullable(image.getPackageVersions())
                .map(map -> map.get(key))
                .orElse(null);
    }

    private boolean versionsArePresent(String currentVersion, String newVersion) {
        return currentVersion != null && newVersion != null;
    }

    @VisibleForTesting
    boolean permitCmAndStackUpgradeByComponentVersion(String currentVersion, String newVersion) {
        boolean result = false;
        if (currentVersion != null && newVersion != null) {
            VersionPrefix prefixMatcher = new VersionPrefix();
            if (prefixMatcher.prefixMatch(() -> currentVersion, () -> newVersion, MAJOR)) {
                VersionComparator comparator = new VersionComparator();
                result = comparator.compare(() -> currentVersion, () -> newVersion) < 0;
            }
        }
        return result;
    }

    private boolean permitCmAndStackUpgradeByBuildNumber(Image currentImage, Image image, String buildNumberKey, String currentVersion, String newVersion) {
        return currentVersion.equals(newVersion) && componentBuildNumberComparator.compare(currentImage, image, buildNumberKey);
    }

    public boolean permitExtensionUpgrade(String currentVersion, String newVersion) {
        boolean result = false;
        if (currentVersion == null) {
            // We shall not care care if they do not exists on the old image
            result = true;
        } else if (newVersion != null) {
            VersionPrefix prefixMatcher = new VersionPrefix();
            if (prefixMatcher.prefixMatch(() -> currentVersion, () -> newVersion, MAJOR)) {
                VersionComparator comparator = new VersionComparator();
                // even if there is no new extension version we shall alow to upgrade to this image
                result = comparator.compare(() -> currentVersion, () -> newVersion) <= 0;
            }
        }
        return result;
    }
}
