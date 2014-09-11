package com.sequenceiq.cloudbreak.service.stack.connector;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;

public interface CloudPlatformConnector {

    StackDescription describeStackWithResources(Stack stack, Credential credential);

    void deleteStack(Stack stack, Credential credential);

    CloudPlatform getCloudPlatform();

    Boolean startAll(Long stackId);

    Boolean stopAll(Long stackId);

}
