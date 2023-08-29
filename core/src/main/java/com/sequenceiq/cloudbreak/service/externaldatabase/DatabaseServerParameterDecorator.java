package com.sequenceiq.cloudbreak.service.externaldatabase;

import java.util.Map;
import java.util.Optional;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseServerParameter;
import com.sequenceiq.common.model.DatabaseType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;

public interface DatabaseServerParameterDecorator {
    void setParameters(DatabaseServerV4StackRequest request, DatabaseServerParameter serverParameter, DetailedEnvironmentResponse env, boolean multiAz);

    default Optional<? extends DatabaseType> getDatabaseType(Map<String, Object> attributes) {
        return Optional.empty();
    }

    CloudPlatform getCloudPlatform();
}
