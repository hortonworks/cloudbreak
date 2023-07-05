package com.sequenceiq.datalake.service.sdx.database;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;

public interface DatabaseServerParameterSetter {
    void setParameters(DatabaseServerV4StackRequest request, SdxCluster sdxCluster);

    CloudPlatform getCloudPlatform();
}
