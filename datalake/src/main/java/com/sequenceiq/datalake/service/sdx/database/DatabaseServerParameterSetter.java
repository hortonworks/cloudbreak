package com.sequenceiq.datalake.service.sdx.database;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

public interface DatabaseServerParameterSetter {
    void setParameters(DatabaseServerV4StackRequest request, SdxDatabaseAvailabilityType availabilityType);

    CloudPlatform getCloudPlatform();
}
