package com.sequenceiq.cloudbreak.service.stack.connector;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionEvent;

public interface ProvisionSetup {

    ProvisionEvent setupProvisioning(Stack stack) throws Exception;

    ProvisionEvent prepareImage(Stack stack) throws Exception;

    ImageStatusResult checkImage(Stack stack) throws Exception;

    CloudPlatform getCloudPlatform();

}
