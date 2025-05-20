package com.sequenceiq.datalake.service.validation.database;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
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

    public boolean isRuntimeVersionAllowedForUpgrade(String runtimeVersion, String targetMajorVersion) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        boolean databaseServerUpgradePossible;
        Optional<String> minRuntimeVersion = dbOverrideConfig.findMinRuntimeVersion(targetMajorVersion);
        if (entitlementService.isPostgresUpgradeExceptionEnabled(accountId)) {
            LOGGER.debug("Enable database upgrade for runtime {} because it's enabled by entitlement.", runtimeVersion);
            databaseServerUpgradePossible = true;
        } else if (StringUtils.hasText(runtimeVersion) && 0 <= versionComparator.compare(() -> runtimeVersion, minRuntimeVersion::get)) {
            LOGGER.debug("The database upgrade is applicable for runtime version {} and DB targetVersion {}", runtimeVersion, targetMajorVersion);
            databaseServerUpgradePossible = true;
        } else {
            LOGGER.warn("The database runtime version is not applicable for runtime version {} and DB targetVersion {}", runtimeVersion, targetMajorVersion);
            databaseServerUpgradePossible = false;
        }
        return databaseServerUpgradePossible;
    }

    public Optional<String> getMinRuntimeVersion(String majorVersion) {
        return dbOverrideConfig.findMinRuntimeVersion(majorVersion);
    }
}
