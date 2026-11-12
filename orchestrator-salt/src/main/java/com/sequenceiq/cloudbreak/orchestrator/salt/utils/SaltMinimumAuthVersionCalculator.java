package com.sequenceiq.cloudbreak.orchestrator.salt.utils;

import java.util.Collection;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.util.VersionComparator;

/**
 * Calculates the {@code minimum_auth_version} salt master option that lets every minion of the cluster authenticate.
 * The option is available from Salt 3006.17 on the 3006 branch and 3007.9 on the 3007 branch.
 * Authentication protocol 3 is available from 3006.12 and 3007.4 respectively; older minions need a relaxed value until they are replaced.
 */
@Component
public class SaltMinimumAuthVersionCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltMinimumAuthVersionCalculator.class);

    private static final int SECURE_AUTH_PROTOCOL = 3;

    private static final int LEGACY_AUTH_PROTOCOL = 2;

    private static final int UNVERSIONED_AUTH_PROTOCOL = 0;

    private static final Versioned SALT_3007 = () -> "3007";

    private static final Versioned MINIMUM_AUTH_VERSION_OPTION_INTRODUCED_IN_3006 = () -> "3006.17";

    private static final Versioned MINIMUM_AUTH_VERSION_OPTION_INTRODUCED_IN_3007 = () -> "3007.9";

    private static final Versioned SECURE_AUTH_PROTOCOL_INTRODUCED_IN_3006 = () -> "3006.12";

    private static final Versioned SECURE_AUTH_PROTOCOL_INTRODUCED_IN_3007 = () -> "3007.4";

    private static final Versioned LEGACY_AUTH_PROTOCOL_INTRODUCED = () -> "3005";

    private final VersionComparator versionComparator = new VersionComparator();

    public Optional<Integer> calculate(Collection<String> saltMasterVersions, Collection<String> saltMinionVersions) {
        if (saltMasterVersions.stream().noneMatch(this::supportsMinimumAuthVersionOption)) {
            LOGGER.debug("None of the salt masters {} supports the minimum_auth_version option", saltMasterVersions);
            return Optional.empty();
        }
        int minimumAuthVersion = saltMinionVersions.stream()
                .mapToInt(this::getAuthProtocol)
                .min()
                .orElse(SECURE_AUTH_PROTOCOL);
        LOGGER.info("Salt master minimum_auth_version is {} for salt minion versions {}", minimumAuthVersion, saltMinionVersions);
        return Optional.of(minimumAuthVersion);
    }

    private boolean supportsMinimumAuthVersionOption(String saltVersion) {
        return StringUtils.isNotBlank(saltVersion) && supportsFeature(saltVersion,
                MINIMUM_AUTH_VERSION_OPTION_INTRODUCED_IN_3006, MINIMUM_AUTH_VERSION_OPTION_INTRODUCED_IN_3007);
    }

    private int getAuthProtocol(String saltVersion) {
        if (StringUtils.isBlank(saltVersion)) {
            LOGGER.warn("Unknown salt minion version, assuming the oldest authentication protocol");
            return UNVERSIONED_AUTH_PROTOCOL;
        } else if (supportsFeature(saltVersion, SECURE_AUTH_PROTOCOL_INTRODUCED_IN_3006, SECURE_AUTH_PROTOCOL_INTRODUCED_IN_3007)) {
            return SECURE_AUTH_PROTOCOL;
        } else if (isAtLeast(saltVersion, LEGACY_AUTH_PROTOCOL_INTRODUCED)) {
            return LEGACY_AUTH_PROTOCOL;
        } else {
            return UNVERSIONED_AUTH_PROTOCOL;
        }
    }

    private boolean supportsFeature(String saltVersion, Versioned introducedIn3006, Versioned introducedIn3007) {
        return isAtLeast(saltVersion, introducedIn3007)
                || (!isAtLeast(saltVersion, SALT_3007) && isAtLeast(saltVersion, introducedIn3006));
    }

    private boolean isAtLeast(String saltVersion, Versioned minimumVersion) {
        return versionComparator.compare(() -> saltVersion, minimumVersion) >= 0;
    }
}
