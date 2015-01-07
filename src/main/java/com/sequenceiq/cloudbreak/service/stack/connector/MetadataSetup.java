package com.sequenceiq.cloudbreak.service.stack.connector;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;

public interface MetadataSetup {

    boolean setupMetadata(Stack stack);

    void addNewNodesToMetadata(Stack stack, Set<Resource> resourceList);

    CloudPlatform getCloudPlatform();

}
