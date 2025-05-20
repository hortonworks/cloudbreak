package com.sequenceiq.cloudbreak.service.upgrade.rds;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.service.database.DbOverrideConfig;
import com.sequenceiq.cloudbreak.util.VersionComparator;

@Component
public class DatabaseUpgradeRuntimeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseUpgradeRuntimeValidator.class);

    private final VersionComparator versionComparator = new VersionComparator();

    @Inject
    private DbOverrideConfig dbOverrideConfig;

    @Inject
    private EntitlementService entitlementService;

    public Optional<String> validateRuntimeVersionForUpgrade(String runtimeVersion, String targetMajorVersion, String accountId) {
        Optional<String> validationFailureMessage = Optional.empty();
        Optional<String> minRuntimeVersion = dbOverrideConfig.findMinRuntimeVersion(targetMajorVersion);
        if (entitlementService.isPostgresUpgradeExceptionEnabled(accountId)) {
            LOGGER.debug("Enable database upgrade for runtime {} because it's enabled by entitlement.", runtimeVersion);
        } else if (StringUtils.hasText(runtimeVersion)
                && minRuntimeVersion.isPresent()
                && 0 <= versionComparator.compare(() -> runtimeVersion, minRuntimeVersion::get)) {
            LOGGER.debug("The database upgrade is applicable for runtime version {} and DB targetVersion {}", runtimeVersion, targetMajorVersion);
        } else {
            String message = String.format("The database upgrade is not permitted for runtime version %s and DB targetVersion %s. " +
                            "The minimum supported runtime version is %s",
                    runtimeVersion, targetMajorVersion, minRuntimeVersion.orElse("not configured"));
            LOGGER.warn(message);
            validationFailureMessage = Optional.of(message);
        }
        return validationFailureMessage;
    }
}
