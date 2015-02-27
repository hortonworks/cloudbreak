package com.sequenceiq.cloudbreak.service.stack.connector;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionEvent;

public interface MetadataSetup {

    ProvisionEvent setupMetadata(Stack stack);

    ProvisionEvent addNewNodesToMetadata(Stack stack, Set<Resource> resourceList, String hostGroup);

    CloudPlatform getCloudPlatform();

}
