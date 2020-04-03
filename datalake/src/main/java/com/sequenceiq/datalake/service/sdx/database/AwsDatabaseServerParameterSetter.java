package com.sequenceiq.datalake.service.sdx.database;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsDatabaseServerV4Parameters;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@Component
public class AwsDatabaseServerParameterSetter implements DatabaseServerParameterSetter {

    @Value("${sdx.db.aws.engineversion:10.6}")
    private String engineVersion;

    @Override
    public void setParameters(DatabaseServerV4StackRequest request, SdxDatabaseAvailabilityType availabilityType) {
        AwsDatabaseServerV4Parameters parameters = new AwsDatabaseServerV4Parameters();
        if (SdxDatabaseAvailabilityType.HA.equals(availabilityType)) {
            parameters.setBackupRetentionPeriod(1);
            parameters.setMultiAZ("true");
        } else if (SdxDatabaseAvailabilityType.NON_HA.equals(availabilityType)) {
            parameters.setBackupRetentionPeriod(0);
            parameters.setMultiAZ("false");
        } else {
            throw new IllegalArgumentException(availabilityType + " database availability type is not supported on AWS.");
        }
        parameters.setEngineVersion(engineVersion);
        request.setAws(parameters);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }
}
