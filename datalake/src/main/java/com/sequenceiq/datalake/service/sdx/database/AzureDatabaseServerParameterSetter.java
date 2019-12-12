package com.sequenceiq.datalake.service.sdx.database;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure.AzureDatabaseServerV4Parameters;

@Component
public class AzureDatabaseServerParameterSetter implements DatabaseServerParameterSetter {

    @Value("${sdx.db.azure.retentionperiod:7}")
    private int retentionPeriod;

    @Value("${sdx.db.azure.geoRedundantBackup:true}")
    private Boolean geoREdundantBackup;

    @Override
    public void setParameters(DatabaseServerV4StackRequest request) {
        AzureDatabaseServerV4Parameters parameters = new AzureDatabaseServerV4Parameters();
        parameters.setBackupRetentionDays(retentionPeriod);
        parameters.setGeoRedundantBackup(geoREdundantBackup);
        request.setAzure(parameters);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
