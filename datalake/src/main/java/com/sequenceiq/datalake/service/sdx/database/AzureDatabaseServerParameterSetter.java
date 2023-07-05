package com.sequenceiq.datalake.service.sdx.database;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.datalake.entity.SdxCluster;
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
    public void setParameters(DatabaseServerV4StackRequest request, SdxCluster sdxCluster) {
        AzureDatabaseServerV4Parameters parameters = new AzureDatabaseServerV4Parameters();
        SdxDatabaseAvailabilityType availabilityType = DatabaseParameterFallbackUtil.getDatabaseAvailabilityType(sdxCluster.getSdxDatabase(),
                sdxCluster.getDatabaseAvailabilityType(), sdxCluster.isCreateDatabase());
        String databaseEngineVersion = DatabaseParameterFallbackUtil.getDatabaseEngineVersion(
                sdxCluster.getSdxDatabase(), sdxCluster.getDatabaseEngineVersion());
        if (SdxDatabaseAvailabilityType.HA.equals(availabilityType)) {
            parameters.setBackupRetentionDays(backupRetentionPeriodHa);
            parameters.setGeoRedundantBackup(geoRedundantBackupHa);
        } else if (SdxDatabaseAvailabilityType.NON_HA.equals(availabilityType)) {
            parameters.setBackupRetentionDays(backupRetentionPeriodNonHa);
            parameters.setGeoRedundantBackup(geoRedundantBackupNonHa);
        } else {
            throw new IllegalArgumentException(availabilityType + " database availability type is not supported on Azure.");
        }
        if (StringUtils.isNotEmpty(databaseEngineVersion)) {
            parameters.setDbVersion(databaseEngineVersion);
        }
        parameters.setAzureDatabaseType(getAzureDatabaseType(sdxCluster.getSdxDatabase()));
        request.setAzure(parameters);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    private AzureDatabaseType getAzureDatabaseType(SdxDatabase sdxDatabase) {
        return sdxDatabase != null ? azureDatabaseAttributesService.getAzureDatabaseType(sdxDatabase) : AzureDatabaseType.SINGLE_SERVER;
    }
}
