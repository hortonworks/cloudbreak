package com.sequenceiq.cloudbreak.javaversion;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.util.VersionComparator;

public interface RuntimeVersionRange {

    VersionComparator VERSION_COMPARATOR = new VersionComparator();

    String from();

    String until();

    default boolean covers(String runtimeVersion) {
        return StringUtils.isNotBlank(runtimeVersion)
                && compareVersions(runtimeVersion, from()) >= 0
                && (StringUtils.isBlank(until()) || compareVersions(runtimeVersion, until()) < 0);
    }

    default String rangeDescription() {
        return "[" + from() + ", " + (StringUtils.isBlank(until()) ? "*" : until()) + ")";
    }

    static int compareVersions(String leftVersion, String rightVersion) {
        return VERSION_COMPARATOR.compare(() -> leftVersion, () -> rightVersion);
    }
}
