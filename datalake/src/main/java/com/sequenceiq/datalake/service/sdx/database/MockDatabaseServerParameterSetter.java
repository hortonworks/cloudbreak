package com.sequenceiq.datalake.service.sdx.database;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@Component
public class MockDatabaseServerParameterSetter implements DatabaseServerParameterSetter {
    @Override
    public void setParameters(DatabaseServerV4StackRequest request, SdxDatabaseAvailabilityType availabilityType, String databaseEngineVersion) {
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.MOCK;
    }
}
