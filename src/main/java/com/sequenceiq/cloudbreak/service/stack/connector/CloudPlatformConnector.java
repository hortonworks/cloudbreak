package com.sequenceiq.cloudbreak.service.stack.connector;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.User;

public interface CloudPlatformConnector {

    StackDescription describeStackWithResources(User user, Stack stack, Credential credential);

    void deleteStack(User user, Stack stack, Credential credential);

    CloudPlatform getCloudPlatform();

    Boolean startAll(User user, Long stackId);

    Boolean stopAll(User user, Long stackId);

}
