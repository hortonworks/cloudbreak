package com.sequenceiq.cloudbreak.service.stack.connector;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;

public interface MetadataSetup {

    void setupMetadata(Stack stack);

    CloudPlatform getCloudPlatform();

}
