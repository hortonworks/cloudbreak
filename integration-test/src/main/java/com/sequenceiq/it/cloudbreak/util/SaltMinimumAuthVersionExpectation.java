package com.sequenceiq.it.cloudbreak.util;

import java.util.Collection;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.util.VersionComparator;

public final class SaltMinimumAuthVersionExpectation {

    private static final VersionComparator VERSION_COMPARATOR = new VersionComparator();

    private static final int UNVERSIONED_AUTH_PROTOCOL = 0;

    private static final int LEGACY_AUTH_PROTOCOL = 2;

    private static final int SECURE_AUTH_PROTOCOL = 3;

    private static final String FIRST_3007_BRANCH_RELEASE = "3007";

    private static final String LEGACY_AUTH_PROTOCOL_RELEASE = "3005";

    private static final String MINIMUM_AUTH_VERSION_OPTION_RELEASE_3006 = "3006.17";

    private static final String MINIMUM_AUTH_VERSION_OPTION_RELEASE_3007 = "3007.9";

    private static final String SECURE_AUTH_PROTOCOL_RELEASE_3006 = "3006.12";

    private static final String SECURE_AUTH_PROTOCOL_RELEASE_3007 = "3007.4";

    private SaltMinimumAuthVersionExpectation() {
    }

    public static Optional<Integer> expectedMinimumAuthVersion(Collection<String> gatewaySaltVersions, Collection<String> allSaltVersions) {
        if (gatewaySaltVersions.stream().noneMatch(SaltMinimumAuthVersionExpectation::hasMinimumAuthVersionOption)) {
            return Optional.empty();
        }
        return Optional.of(allSaltVersions.stream().mapToInt(SaltMinimumAuthVersionExpectation::authProtocolOf).min().orElseThrow());
    }

    private static boolean hasMinimumAuthVersionOption(String saltVersion) {
        return supportsFeature(saltVersion, MINIMUM_AUTH_VERSION_OPTION_RELEASE_3006, MINIMUM_AUTH_VERSION_OPTION_RELEASE_3007);
    }

    private static int authProtocolOf(String saltVersion) {
        if (StringUtils.isBlank(saltVersion)) {
            return UNVERSIONED_AUTH_PROTOCOL;
        } else if (supportsFeature(saltVersion, SECURE_AUTH_PROTOCOL_RELEASE_3006, SECURE_AUTH_PROTOCOL_RELEASE_3007)) {
            return SECURE_AUTH_PROTOCOL;
        } else if (atLeast(saltVersion, LEGACY_AUTH_PROTOCOL_RELEASE)) {
            return LEGACY_AUTH_PROTOCOL;
        } else {
            return UNVERSIONED_AUTH_PROTOCOL;
        }
    }

    private static boolean supportsFeature(String saltVersion, String firstReleaseOn3006Branch, String firstReleaseOn3007Branch) {
        if (StringUtils.isBlank(saltVersion)) {
            return false;
        }
        return atLeast(saltVersion, firstReleaseOn3007Branch)
                || (isBeforeBranch3007(saltVersion) && atLeast(saltVersion, firstReleaseOn3006Branch));
    }

    private static boolean isBeforeBranch3007(String saltVersion) {
        return !atLeast(saltVersion, FIRST_3007_BRANCH_RELEASE);
    }

    private static boolean atLeast(String saltVersion, String release) {
        return VERSION_COMPARATOR.compare(() -> saltVersion, () -> release) >= 0;
    }
}
