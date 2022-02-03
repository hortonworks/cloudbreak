package com.sequenceiq.cloudbreak.service.externaldatabase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseServerParameter;
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

    @Override
    public void setParameters(DatabaseServerV4StackRequest request, DatabaseServerParameter serverParameter) {
        AzureDatabaseServerV4Parameters parameters = new AzureDatabaseServerV4Parameters();
        if (serverParameter.isHighlyAvailable()) {
            parameters.setBackupRetentionDays(retentionPeriodHa);
            parameters.setGeoRedundantBackup(geoRedundantBackupHa);
        } else {
            parameters.setBackupRetentionDays(retentionPeriodNonHa);
            parameters.setGeoRedundantBackup(geoRedundantBackupNonHa);
        }
        parameters.setBackupRetentionDays(retentionPeriodHa);
        parameters.setGeoRedundantBackup(geoRedundantBackupHa);
        parameters.setDbVersion(serverParameter.getEngineVersion());
        request.setAzure(parameters);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
