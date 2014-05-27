package com.sequenceiq.provisioning.service;

import com.sequenceiq.provisioning.controller.json.StackResult;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.Credential;
import com.sequenceiq.provisioning.domain.Stack;
import com.sequenceiq.provisioning.domain.StackDescription;
import com.sequenceiq.provisioning.domain.User;

public interface ProvisionService {

    StackResult createStack(User user, Stack stack, Credential credential);

    StackDescription describeStack(User user, Stack stack, Credential credential);

    StackDescription describeStackWithResources(User user, Stack stack, Credential credential);

    void deleteStack(User user, Stack stack, Credential credential);

    CloudPlatform getCloudPlatform();

}
