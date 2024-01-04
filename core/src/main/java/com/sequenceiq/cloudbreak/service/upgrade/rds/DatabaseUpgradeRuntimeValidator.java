package com.sequenceiq.cloudbreak.service.upgrade.rds;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.util.VersionComparator;

@Component
public class DatabaseUpgradeRuntimeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseUpgradeRuntimeValidator.class);

    private final VersionComparator versionComparator = new VersionComparator();

    @Value("${cb.db.override.minRuntimeVersion}")
    private String minRuntimeVersion;

    @Inject
    private EntitlementService entitlementService;

    public Optional<String> validateRuntimeVersionForUpgrade(String runtimeVersion, String accountId) {
        Optional<String> validationFailureMessage = Optional.empty();
        if (entitlementService.isPostgresUpgradeExceptionEnabled(accountId)) {
            LOGGER.debug("Enable database upgrade for runtime {} because it's enabled by entitlement.", runtimeVersion);
        } else if (StringUtils.hasText(runtimeVersion) && 0 <= versionComparator.compare(() -> runtimeVersion, () -> minRuntimeVersion)) {
            LOGGER.debug("The database upgrade is applicable for runtime version {}", runtimeVersion);
        } else {
            String message = String.format("The database upgrade is not permitted for runtime version %s. The minimum supported runtime version is %s",
                    runtimeVersion, minRuntimeVersion);
            LOGGER.warn(message);
            validationFailureMessage = Optional.of(message);
        }
        return validationFailureMessage;
    }
}
