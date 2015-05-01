package com.sequenceiq.cloudbreak.service.stack.connector;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;

public interface MetadataSetup {

    Set<CoreInstanceMetaData> collectMetadata(Stack stack);

    Set<CoreInstanceMetaData> collectNewMetadata(Stack stack, Set<Resource> resourceList, String instanceGroup);

    CloudPlatform getCloudPlatform();

}
