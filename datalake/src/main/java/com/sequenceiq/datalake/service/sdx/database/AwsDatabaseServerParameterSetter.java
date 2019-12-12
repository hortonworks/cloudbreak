package com.sequenceiq.datalake.service.sdx.database;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsDatabaseServerV4Parameters;

@Component
public class AwsDatabaseServerParameterSetter implements DatabaseServerParameterSetter {

    @Value("${sdx.db.aws.retentionperiod:1}")
    private int retentionPeriod;

    @Value("${sdx.db.aws.multiaz:true}")
    private String multiAz;

    @Value("${sdx.db.aws.engineversion:10.6}")
    private String engineVersion;

    @Override
    public void setParameters(DatabaseServerV4StackRequest request) {
        AwsDatabaseServerV4Parameters parameters = new AwsDatabaseServerV4Parameters();
        parameters.setBackupRetentionPeriod(retentionPeriod);
        parameters.setMultiAZ(multiAz);
        parameters.setEngineVersion(engineVersion);
        request.setAws(parameters);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }
}
