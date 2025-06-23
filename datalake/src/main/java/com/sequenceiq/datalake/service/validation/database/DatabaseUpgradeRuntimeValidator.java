package com.sequenceiq.datalake.service.validation.database;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
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

    public String getMinRuntimeVersion() {
        return minRuntimeVersion;
    }

    public boolean isRuntimeVersionAllowedForUpgrade(String runtimeVersion) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        boolean databaseServerUpgradePossible;
        if (entitlementService.isPostgresUpgradeExceptionEnabled(accountId)) {
            LOGGER.debug("Enable database upgrade for runtime {} because it's enabled by entitlement.", runtimeVersion);
            databaseServerUpgradePossible = true;
        } else if (StringUtils.hasText(runtimeVersion) && 0 <= versionComparator.compare(() -> runtimeVersion, () -> minRuntimeVersion)) {
            LOGGER.debug("The database upgrade is applicable for runtime version {}", runtimeVersion);
            databaseServerUpgradePossible = true;
        } else {
            LOGGER.warn("The database runtime version is not applicable for runtime version {}", runtimeVersion);
            databaseServerUpgradePossible = false;
        }
        return databaseServerUpgradePossible;
    }

}
