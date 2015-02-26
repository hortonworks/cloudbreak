package com.sequenceiq.cloudbreak.service.stack.connector;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionEvent;

public interface ProvisionSetup {

    ProvisionEvent setupProvisioning(Stack stack) throws Exception;

    String preProvisionCheck(Stack stack);

    CloudPlatform getCloudPlatform();

}
