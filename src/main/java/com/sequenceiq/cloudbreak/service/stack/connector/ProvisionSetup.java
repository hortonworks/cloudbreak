package com.sequenceiq.cloudbreak.service.stack.connector;

import java.util.Map;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;

public interface ProvisionSetup {

    void setupProvisioning(Stack stack);

    Optional<String> preProvisionCheck(Stack stack);

    CloudPlatform getCloudPlatform();

    Map<String, Object> getSetupProperties(Stack stack);

    Map<String, String> getUserDataProperties(Stack stack);

}
