package com.sequenceiq.cloudbreak.service.stack;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.User;

public interface ProvisionService {

    void createStack(User user, Stack stack, Credential credential);

    StackDescription describeStack(User user, Stack stack, Credential credential);

    StackDescription describeStackWithResources(User user, Stack stack, Credential credential);

    void deleteStack(User user, Stack stack, Credential credential);

    CloudPlatform getCloudPlatform();

    Boolean startAll(User user, Long stackId);

    Boolean stopAll(User user, Long stackId);

}
