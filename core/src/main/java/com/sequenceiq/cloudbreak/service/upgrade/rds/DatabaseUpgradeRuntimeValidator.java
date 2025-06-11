package com.sequenceiq.cloudbreak.service.upgrade.rds;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.database.DbOverrideConfig;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.model.DatabaseCapabilityType;
import com.sequenceiq.environment.api.v1.platformresource.EnvironmentPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDatabaseCapabilitiesResponse;

@Component
public class DatabaseUpgradeRuntimeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseUpgradeRuntimeValidator.class);

    private final VersionComparator versionComparator = new VersionComparator();

    @Inject
    private DbOverrideConfig dbOverrideConfig;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private EnvironmentPlatformResourceEndpoint environmentPlatformResourceEndpoint;

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

    public Optional<String> validateTargetMajorVersionAvailability(String targetMajorVersion, String currentEngineVersion, StackView stack) {
        String environmentCrn = stack.getEnvironmentCrn();
        String region = stack.getRegion();
        String platformVariant = stack.getPlatformVariant();
        String availabilityZone = stack.getAvailabilityZone();
        String architecture = stack.getArchitecture().getName();

        DatabaseCapabilityType databaseType =
                CloudPlatform.AZURE.name().equalsIgnoreCase(stack.getCloudPlatform()) ?
                        DatabaseCapabilityType.AZURE_FLEXIBLE : DatabaseCapabilityType.DEFAULT;

        PlatformDatabaseCapabilitiesResponse databaseCapabilities = environmentPlatformResourceEndpoint.
                getDatabaseCapabilities(environmentCrn, region, platformVariant, availabilityZone, databaseType, architecture);

        Map<String, Map<String, List<String>>> regionUpgradeVersions = databaseCapabilities.getRegionUpgradeVersions();
        return Optional.ofNullable(regionUpgradeVersions)
                .map(versions -> versions.get(stack.getRegion()))
                .map(upgradeVersions -> upgradeVersions.get(currentEngineVersion))
                .filter(availableVersions -> !availableVersions.contains(targetMajorVersion))
                .map(availableVersions -> {
                    String message = String.format(
                            "The DB target major version %s is not supported in region %s for platform %s. Supported versions are: %s",
                            targetMajorVersion, stack.getRegion(), stack.getPlatformVariant(), availableVersions);
                    LOGGER.warn(message);
                    return message;
                });
    }
}
