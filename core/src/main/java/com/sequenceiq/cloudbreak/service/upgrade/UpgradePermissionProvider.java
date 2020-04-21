package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.cloud.PrefixMatchLength.MAINTENANCE;
import static com.sequenceiq.cloudbreak.cloud.PrefixMatchLength.MINOR;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.VersionPrefix;

@Service
public class UpgradePermissionProvider {

    public boolean permitCmAndStackUpgrade(String currentVersion, String newVersion) {
        boolean result = false;
        if (currentVersion != null && newVersion != null) {
            VersionPrefix prefixMatcher = new VersionPrefix();
            if (prefixMatcher.prefixMatch(() -> currentVersion, () -> newVersion, MINOR)) {
                VersionComparator comparator = new VersionComparator();
                result = comparator.compare(() -> currentVersion, () -> newVersion) < 0;
            }
        }
        return result;
    }

    public boolean permitSaltUpgrade(String currentVersion, String newVersion) {
        boolean result = false;
        if (currentVersion != null && newVersion != null) {
            VersionPrefix prefixMatcher = new VersionPrefix();
            result = prefixMatcher.prefixMatch(() -> currentVersion, () -> newVersion, MAINTENANCE);
        }
        return result;
    }

    public boolean permitExtensionUpgrade(String currentVersion, String newVersion) {
        boolean result = false;
        if (currentVersion == null) {
            // We shall not care care if they do not exists on the old image
            result = true;
        } else if (newVersion != null) {
            VersionPrefix prefixMatcher = new VersionPrefix();
            if (prefixMatcher.prefixMatch(() -> currentVersion, () -> newVersion, MINOR)) {
                VersionComparator comparator = new VersionComparator();
                // even if there is no new extension version we shall alow to upgrade to this image
                result = comparator.compare(() -> currentVersion, () -> newVersion) <= 0;
            }
        }
        return result;
    }
}
