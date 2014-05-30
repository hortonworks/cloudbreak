package com.sequenceiq.provisioning.service;

import org.springframework.scheduling.annotation.Async;

import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.Credential;
import com.sequenceiq.provisioning.domain.Stack;
import com.sequenceiq.provisioning.domain.StackDescription;
import com.sequenceiq.provisioning.domain.User;

public interface ProvisionService {

    @Async
    void createStack(User user, Stack stack, Credential credential);

    StackDescription describeStack(User user, Stack stack, Credential credential);

    StackDescription describeStackWithResources(User user, Stack stack, Credential credential);

    void deleteStack(User user, Stack stack, Credential credential);

    CloudPlatform getCloudPlatform();

    Boolean startAll(User user, Long stackId);

    Boolean stopAll(User user, Long stackId);

}
