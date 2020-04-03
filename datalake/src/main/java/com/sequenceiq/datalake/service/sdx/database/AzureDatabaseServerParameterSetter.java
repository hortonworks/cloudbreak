package com.sequenceiq.datalake.service.sdx.database;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure.AzureDatabaseServerV4Parameters;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@Component
public class AzureDatabaseServerParameterSetter implements DatabaseServerParameterSetter {

    private static final int RETENTION_PERIOD = 7;

    @Override
    public void setParameters(DatabaseServerV4StackRequest request, SdxDatabaseAvailabilityType availabilityType) {
        AzureDatabaseServerV4Parameters parameters = new AzureDatabaseServerV4Parameters();
        if (SdxDatabaseAvailabilityType.HA.equals(availabilityType)) {
            parameters.setBackupRetentionDays(RETENTION_PERIOD);
            parameters.setGeoRedundantBackup(true);
        } else if (SdxDatabaseAvailabilityType.NON_HA.equals(availabilityType)) {
            parameters.setBackupRetentionDays(RETENTION_PERIOD);
            parameters.setGeoRedundantBackup(false);
        } else {
            throw new IllegalArgumentException(availabilityType + " database availability type is not supported on Azure.");
        }
        request.setAzure(parameters);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
