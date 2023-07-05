package com.sequenceiq.cloudbreak.service.externaldatabase;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseServerParameter;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;

public interface DatabaseServerParameterDecorator {
    void setParameters(DatabaseServerV4StackRequest request, DatabaseServerParameter serverParameter);

    CloudPlatform getCloudPlatform();
}
