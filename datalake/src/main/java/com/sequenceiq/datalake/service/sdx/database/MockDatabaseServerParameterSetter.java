package com.sequenceiq.datalake.service.sdx.database;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;

@Component
public class MockDatabaseServerParameterSetter implements DatabaseServerParameterSetter {
    @Override
    public void setParameters(DatabaseServerV4StackRequest request, SdxCluster sdxCluster, DetailedEnvironmentResponse env, String initiatorUserCrn) {
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.MOCK;
    }
}
