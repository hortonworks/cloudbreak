package com.sequenceiq.cloudbreak.service.stack.connector;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;

public interface ProvisionSetup {

    void setupProvisioning(Stack stack);

    Optional<String> preProvisionCheck(Stack stack);

    CloudPlatform getCloudPlatform();

}
