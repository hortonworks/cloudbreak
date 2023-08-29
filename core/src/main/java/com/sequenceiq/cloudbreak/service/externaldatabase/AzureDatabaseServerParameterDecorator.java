package com.sequenceiq.cloudbreak.service.externaldatabase;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType.HA;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType.NON_HA;
import static com.sequenceiq.common.model.AzureHighAvailabiltyMode.DISABLED;
import static com.sequenceiq.common.model.AzureHighAvailabiltyMode.SAME_ZONE;
import static com.sequenceiq.common.model.AzureHighAvailabiltyMode.ZONE_REDUNDANT;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseServerParameter;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.common.model.AzureHighAvailabiltyMode;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.platformresource.EnvironmentPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDatabaseCapabilitiesResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure.AzureDatabaseServerV4Parameters;

@Component
public class AzureDatabaseServerParameterDecorator implements DatabaseServerParameterDecorator {

    @Value("${cb.azure.externaldatabase.ha.retentionperiod}")
    private int retentionPeriodHa;

    @Value("${cb.azure.externaldatabase.ha.georedundantbackup}")
    private Boolean geoRedundantBackupHa;

    @Value("${cb.azure.externaldatabase.nonha.retentionperiod}")
    private int retentionPeriodNonHa;

    @Value("${cb.azure.externaldatabase.nonha.georedundantbackup}")
    private Boolean geoRedundantBackupNonHa;

    @Inject
    private EnvironmentPlatformResourceEndpoint environmentPlatformResourceEndpoint;

    @Override
    public Optional<AzureDatabaseType> getDatabaseType(Map<String, Object> attributes) {
        return Optional.ofNullable(getAzureDatabaseType(attributes));
    }

    @Override
    public void setParameters(DatabaseServerV4StackRequest request, DatabaseServerParameter serverParameter, DetailedEnvironmentResponse env, boolean multiAz) {
        AzureDatabaseServerV4Parameters parameters = new AzureDatabaseServerV4Parameters();
        DatabaseAvailabilityType availabilityType = serverParameter.getAvailabilityType();
        AzureDatabaseType azureDatabaseType = getAzureDatabaseType(serverParameter.getAttributes());
        parameters.setAzureDatabaseType(azureDatabaseType);
        // Until flexible is not a default. Remove this statement after that
        if (multiAz && azureDatabaseType == AzureDatabaseType.FLEXIBLE_SERVER) {
            List<String> zones = env.getNetwork().getAzure().getAvailabilityZones().stream().toList();
            parameters.setAvailabilityZone(getAvailabilityZone(zones));
            AzureHighAvailabiltyMode highAvailabilityMode = getHighAvailabilityMode(availabilityType, isZoneRedundantHaEnabled(env));
            parameters.setHighAvailabilityMode(highAvailabilityMode);
            parameters.setStandbyAvailabilityZone(getStandByAvailabilityZone(zones, highAvailabilityMode));
        } else {
            parameters.setHighAvailabilityMode(getHighAvailabilityMode(availabilityType, false));
        }
        parameters.setBackupRetentionDays(getBackupRetentionPeriod(availabilityType));
        parameters.setGeoRedundantBackup(getGeoRedundantBackup(availabilityType));
        parameters.setDbVersion(serverParameter.getEngineVersion());
        request.setAzure(parameters);
    }

    private boolean isZoneRedundantHaEnabled(DetailedEnvironmentResponse env) {
        boolean zoneRedundantHaEnabled = false;
        PlatformDatabaseCapabilitiesResponse databaseCapabilities = getDatabaseCapabilities(env);
        List<String> enabledRegions = databaseCapabilities.getIncludedRegions().get(ZONE_REDUNDANT.name());
        if (enabledRegions != null) {
            if (enabledRegions.contains(env.getLocation().getName())) {
                zoneRedundantHaEnabled = true;
            }
        }
        return zoneRedundantHaEnabled;
    }

    private PlatformDatabaseCapabilitiesResponse getDatabaseCapabilities(DetailedEnvironmentResponse env) {
        return environmentPlatformResourceEndpoint.getDatabaseCapabilities(
                env.getCrn(),
                env.getLocation().getName(),
                env.getCloudPlatform(),
                null);
    }

    private AzureHighAvailabiltyMode getHighAvailabilityMode(DatabaseAvailabilityType availabilityType, boolean zoneRedundantEnabled) {
        if (HA.equals(availabilityType)) {
            if (zoneRedundantEnabled) {
                return ZONE_REDUNDANT;
            } else {
                return SAME_ZONE;
            }
        } else if (NON_HA.equals(availabilityType)) {
            return DISABLED;
        } else {
            throw unkownDatabaseAvailabilityType(availabilityType);
        }
    }

    private boolean getGeoRedundantBackup(DatabaseAvailabilityType availabilityType) {
        if (HA.equals(availabilityType)) {
            return geoRedundantBackupHa;
        } else if (NON_HA.equals(availabilityType)) {
            return geoRedundantBackupNonHa;
        } else {
            throw unkownDatabaseAvailabilityType(availabilityType);
        }
    }

    private int getBackupRetentionPeriod(DatabaseAvailabilityType availabilityType) {
        if (HA.equals(availabilityType)) {
            return retentionPeriodHa;
        } else if (NON_HA.equals(availabilityType)) {
            return retentionPeriodNonHa;
        } else {
            throw unkownDatabaseAvailabilityType(availabilityType);
        }
    }

    private IllegalArgumentException unkownDatabaseAvailabilityType(DatabaseAvailabilityType availabilityType) {
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
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    private AzureDatabaseType getAzureDatabaseType(Map<String, Object> attributes) {
        String dbTypeStr = (String) attributes.get(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY);
        return AzureDatabaseType.safeValueOf(dbTypeStr);
    }

}
