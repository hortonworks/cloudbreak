package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.cloud.PrefixMatchLength.MAJOR;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.VersionPrefix;

@Component
class ComponentVersionComparator {

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
}
