package com.sequenceiq.datalake.service.sdx.database;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;

@Component
public class MockDatabaseServerParameterSetter implements DatabaseServerParameterSetter {
    @Override
    public void setParameters(DatabaseServerV4StackRequest request, SdxCluster sdxCluster) {
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.MOCK;
    }
}
