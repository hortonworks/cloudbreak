package com.sequenceiq.datalake.service.sdx.database;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.common.model.AzureHighAvailabiltyMode;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure.AzureDatabaseServerV4Parameters;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@Component
public class AzureDatabaseServerParameterSetter implements DatabaseServerParameterSetter {

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

    @Override
    public void setParameters(DatabaseServerV4StackRequest request, SdxDatabase sdxDatabase) {
        AzureDatabaseServerV4Parameters parameters = new AzureDatabaseServerV4Parameters();
        SdxDatabaseAvailabilityType availabilityType = DatabaseParameterFallbackUtil.getDatabaseAvailabilityType(
                sdxDatabase.getDatabaseAvailabilityType(), sdxDatabase.isCreateDatabase());
        String databaseEngineVersion = sdxDatabase.getDatabaseEngineVersion();
        if (SdxDatabaseAvailabilityType.HA.equals(availabilityType)) {
            parameters.setBackupRetentionDays(backupRetentionPeriodHa);
            parameters.setGeoRedundantBackup(geoRedundantBackupHa);
            parameters.setHighAvailabilityMode(AzureHighAvailabiltyMode.SAME_ZONE);
        } else if (SdxDatabaseAvailabilityType.NON_HA.equals(availabilityType)) {
            parameters.setBackupRetentionDays(backupRetentionPeriodNonHa);
            parameters.setGeoRedundantBackup(geoRedundantBackupNonHa);
            parameters.setHighAvailabilityMode(AzureHighAvailabiltyMode.DISABLED);
        } else {
            throw new IllegalArgumentException(availabilityType + " database availability type is not supported on Azure.");
        }
        if (StringUtils.isNotEmpty(databaseEngineVersion)) {
            parameters.setDbVersion(databaseEngineVersion);
        }
        parameters.setAzureDatabaseType(getAzureDatabaseType(sdxDatabase));
        request.setAzure(parameters);
    }

    @Override
    public Optional<AzureDatabaseType> getDatabaseType(SdxDatabase sdxDatabase) {
        return Optional.of(getAzureDatabaseType(sdxDatabase));
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    private AzureDatabaseType getAzureDatabaseType(SdxDatabase sdxDatabase) {
        return sdxDatabase != null ? azureDatabaseAttributesService.getAzureDatabaseType(sdxDatabase) : AzureDatabaseType.SINGLE_SERVER;
    }
}
