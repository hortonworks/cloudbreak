package com.sequenceiq.datalake.service.sdx.database;

import static com.sequenceiq.common.model.AzureDatabaseType.FLEXIBLE_SERVER;
import static com.sequenceiq.common.model.AzureDatabaseType.SINGLE_SERVER;
import static com.sequenceiq.common.model.AzureHighAvailabiltyMode.DISABLED;
import static com.sequenceiq.common.model.AzureHighAvailabiltyMode.SAME_ZONE;
import static com.sequenceiq.common.model.AzureHighAvailabiltyMode.ZONE_REDUNDANT;
import static com.sequenceiq.common.model.DatabaseCapabilityType.AZURE_FLEXIBLE;
import static com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType.HA;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.common.model.AzureHighAvailabiltyMode;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.platformresource.EnvironmentPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDatabaseCapabilitiesResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure.AzureDatabaseServerV4Parameters;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@Component
public class AzureDatabaseServerParameterSetter implements DatabaseServerParameterSetter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDatabaseServerParameterSetter.class);

    @VisibleForTesting
    @Value("${sdx.db.azure.ha.backupretentionperiod}")
    int backupRetentionPeriodHa;

    @VisibleForTesting
    @Value("${sdx.db.azure.nonha.backupretentionperiod}")
    int backupRetentionPeriodNonHa;

    @VisibleForTesting
    @Value("${sdx.db.azure.ha.georedundantbackup}")
    boolean geoRedundantBackupHa;

    @VisibleForTesting
    @Value("${sdx.db.azure.nonha.georedundantbackup}")
    boolean geoRedundantBackupNonHa;

    @Inject
    private AzureDatabaseAttributesService azureDatabaseAttributesService;

    @Inject
    private EnvironmentPlatformResourceEndpoint environmentPlatformResourceEndpoint;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public void setParameters(DatabaseServerV4StackRequest request, SdxCluster sdxCluster, DetailedEnvironmentResponse env, String initiatorUserCrn) {
        SdxDatabase sdxDatabase = sdxCluster.getSdxDatabase();
        AzureDatabaseServerV4Parameters parameters = new AzureDatabaseServerV4Parameters();
        SdxDatabaseAvailabilityType availabilityType = DatabaseParameterInitUtil.getDatabaseAvailabilityType(
                sdxDatabase.getDatabaseAvailabilityType(), sdxDatabase.isCreateDatabase());
        String databaseEngineVersion = sdxDatabase.getDatabaseEngineVersion();
        // Until flexible is not a default. Remove this statement after that
        AzureDatabaseType azureDatabaseType = getAzureDatabaseType(sdxDatabase);
        parameters.setAzureDatabaseType(azureDatabaseType);
        parameters.setFlexibleServerDelegatedSubnetId(azureDatabaseAttributesService.getFlexibleServerDelegatedSubnetId(sdxDatabase));
        if (sdxCluster.isEnableMultiAz() && FLEXIBLE_SERVER.equals(azureDatabaseType)) {
            List<String> zones = env.getNetwork().getAzure().getAvailabilityZones().stream().toList();
            parameters.setAvailabilityZone(getAvailabilityZone(zones));
            AzureHighAvailabiltyMode highAvailabilityMode =
                    getHighAvailabilityMode(availabilityType, isZoneRedundantHaEnabled(env, initiatorUserCrn));
            parameters.setHighAvailabilityMode(highAvailabilityMode);
            parameters.setStandbyAvailabilityZone(getStandByAvailabilityZone(zones, highAvailabilityMode));
        } else {
            parameters.setHighAvailabilityMode(getHighAvailabilityMode(availabilityType, false));
        }
        parameters.setBackupRetentionDays(getBackupRetentionPeriod(availabilityType));
        parameters.setGeoRedundantBackup(isGeoRedundantBackup(availabilityType));
        parameters.setDbVersion(StringUtils.isNotEmpty(databaseEngineVersion) ? databaseEngineVersion : null);
        request.setAzure(parameters);
    }

    @Override
    public void validate(DatabaseServerV4StackRequest request, SdxCluster sdxCluster, DetailedEnvironmentResponse env, String initiatorUserCrn) {
        AzureDatabaseServerV4Parameters azure = request.getAzure();
        boolean localDevelopment = entitlementService.localDevelopment(env.getAccountId());
        if (sdxCluster.isEnableMultiAz() && azure != null && !localDevelopment) {
            if (!sdxCluster.getSdxDatabase().hasExternalDatabase()) {
                String message = "Azure Data Lake requested in multi availability zone setup must use external database.";
                LOGGER.debug(message);
                throw new BadRequestException(message);
            } else if (SdxDatabaseAvailabilityType.NON_HA.equals(sdxCluster.getSdxDatabase().getDatabaseAvailabilityType())) {
                String message = String.format("Non HA Database is not supported for Azure multi availability zone Data Hubs.");
                LOGGER.debug(message);
                throw new BadRequestException(message);
            } else if (!FLEXIBLE_SERVER.equals(azure.getAzureDatabaseType())) {
                String message = "Azure Data Lake requested in multi availability zone setup must use Flexible server.";
                LOGGER.debug(message);
                throw new BadRequestException(message);
            } else if (!ZONE_REDUNDANT.equals(azure.getHighAvailabilityMode())) {
                String region = env.getLocation().getName();
                String message = String.format("Azure Data Lake requested in multi availability zone setup " +
                        "must use Zone redundant Flexible server and the %s region currently does not support that. " +
                        "You can see the limitations on the following url https://learn.microsoft.com/en-us/azure/postgresql/flexible-server/overview. " +
                        "Please contact Microsoft support that you need Zone Redundant Flexible Server option in the given region.", region);
                LOGGER.debug(message);
                throw new BadRequestException(message);
            }
        }
    }

    private boolean isZoneRedundantHaEnabled(DetailedEnvironmentResponse env, String initiatorUserCrn) {
        boolean zoneRedundantHaEnabled = false;
        PlatformDatabaseCapabilitiesResponse databaseCapabilities = getDatabaseCapabilities(env, initiatorUserCrn);
        List<String> enabledRegions = databaseCapabilities.getIncludedRegions().get(ZONE_REDUNDANT.name());
        if (enabledRegions != null) {
            if (enabledRegions.contains(env.getLocation().getName())) {
                zoneRedundantHaEnabled = true;
            }
        }
        return zoneRedundantHaEnabled;
    }

    private PlatformDatabaseCapabilitiesResponse getDatabaseCapabilities(DetailedEnvironmentResponse env, String initiatorUserCrn) {
        return ThreadBasedUserCrnProvider.doAs(initiatorUserCrn, () ->
                environmentPlatformResourceEndpoint.getDatabaseCapabilities(
                        env.getCrn(),
                        env.getLocation().getName(),
                        env.getCloudPlatform(),
                        null,
                        AZURE_FLEXIBLE,
                        null));
    }

    private AzureHighAvailabiltyMode getHighAvailabilityMode(SdxDatabaseAvailabilityType availabilityType, boolean zoneRedundantEnabled) {
        if (HA.equals(availabilityType)) {
            if (zoneRedundantEnabled) {
                return ZONE_REDUNDANT;
            } else {
                return SAME_ZONE;
            }
        } else if (SdxDatabaseAvailabilityType.NON_HA.equals(availabilityType)) {
            return DISABLED;
        } else {
            throw unkownDatabaseAvailabilityType(availabilityType);
        }
    }

    private boolean isGeoRedundantBackup(SdxDatabaseAvailabilityType availabilityType) {
        if (HA.equals(availabilityType)) {
            return geoRedundantBackupHa;
        } else if (SdxDatabaseAvailabilityType.NON_HA.equals(availabilityType)) {
            return geoRedundantBackupNonHa;
        } else {
            throw unkownDatabaseAvailabilityType(availabilityType);
        }
    }

    private int getBackupRetentionPeriod(SdxDatabaseAvailabilityType availabilityType) {
        if (HA.equals(availabilityType)) {
            return backupRetentionPeriodHa;
        } else if (SdxDatabaseAvailabilityType.NON_HA.equals(availabilityType)) {
            return backupRetentionPeriodNonHa;
        } else {
            throw unkownDatabaseAvailabilityType(availabilityType);
        }
    }

    private IllegalArgumentException unkownDatabaseAvailabilityType(SdxDatabaseAvailabilityType availabilityType) {
        return new IllegalArgumentException(availabilityType + " database availability type is not supported on Azure.");
    }

    private String getAvailabilityZone(List<String> zones) {
        if (zones != null && !zones.isEmpty()) {
            return zones.get(0);
        }
        return null;
    }

    private String getStandByAvailabilityZone(List<String> zones, AzureHighAvailabiltyMode highAvailabilityMode) {
        if (ZONE_REDUNDANT.equals(highAvailabilityMode) && zones != null && !zones.isEmpty() && zones.size() > 1) {
            return zones.get(1);
        }
        return null;
    }

    @Override
    public Optional<AzureDatabaseType> getDatabaseType(SdxDatabase sdxDatabase) {
        return Optional.of(getAzureDatabaseType(sdxDatabase));
    }

    @Override
    public Optional<SdxDatabase> updateVersionRelatedDatabaseParameters(SdxDatabase sdxDatabase, String dbVersion) {
        return azureDatabaseAttributesService.updateVersionRelatedDatabaseParams(sdxDatabase, dbVersion);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    private AzureDatabaseType getAzureDatabaseType(SdxDatabase sdxDatabase) {
        return sdxDatabase != null ? azureDatabaseAttributesService.getAzureDatabaseType(sdxDatabase) : SINGLE_SERVER;
    }
}
