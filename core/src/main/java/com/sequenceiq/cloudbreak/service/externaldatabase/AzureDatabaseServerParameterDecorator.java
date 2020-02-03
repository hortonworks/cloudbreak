package com.sequenceiq.cloudbreak.service.externaldatabase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseServerParameter;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure.AzureDatabaseServerV4Parameters;

@Component
public class AzureDatabaseServerParameterDecorator implements DatabaseServerParameterDecorator {

    @Value("${cb.azure.externaldatabase.retentionperiod:7}")
    private int retentionPeriod;

    @Value("${cb.azure.externaldatabase.geoRedundantBackup:true}")
    private Boolean geoRedundantBackup;

    @Override
    public void setParameters(DatabaseServerV4StackRequest request, DatabaseServerParameter serverParameter) {
        AzureDatabaseServerV4Parameters parameters = new AzureDatabaseServerV4Parameters();
        parameters.setBackupRetentionDays(retentionPeriod);
        parameters.setGeoRedundantBackup(geoRedundantBackup);
        request.setAzure(parameters);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
