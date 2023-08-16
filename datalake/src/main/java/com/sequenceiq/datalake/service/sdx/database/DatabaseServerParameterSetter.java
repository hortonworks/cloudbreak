package com.sequenceiq.datalake.service.sdx.database;

import java.util.Optional;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.DatabaseType;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;

public interface DatabaseServerParameterSetter {
    void setParameters(DatabaseServerV4StackRequest request, SdxCluster sdxCluster, DetailedEnvironmentResponse env, String initiatorUserCrn);

    default Optional<? extends DatabaseType> getDatabaseType(SdxDatabase sdxDatabase) {
        return Optional.empty();
    }

    CloudPlatform getCloudPlatform();
}
