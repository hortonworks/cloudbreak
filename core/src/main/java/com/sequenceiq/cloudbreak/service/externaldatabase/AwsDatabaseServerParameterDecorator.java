package com.sequenceiq.cloudbreak.service.externaldatabase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseServerParameter;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsDatabaseServerV4Parameters;

@Component
public class AwsDatabaseServerParameterDecorator implements DatabaseServerParameterDecorator {

    @Value("${cb.aws.externaldatabase.retentionperiod:1}")
    private int retentionPeriod;

    @Value("${cb.aws.externaldatabase.engineversion:10.6}")
    private String engineVersion;

    @Override
    public void setParameters(DatabaseServerV4StackRequest request, DatabaseServerParameter serverParameter) {
        AwsDatabaseServerV4Parameters parameters = new AwsDatabaseServerV4Parameters();
        parameters.setBackupRetentionPeriod(retentionPeriod);
        parameters.setMultiAZ(Boolean.toString(serverParameter.isHighlyAvailable()));
        parameters.setEngineVersion(engineVersion);
        request.setAws(parameters);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }
}
