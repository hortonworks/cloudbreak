package com.sequenceiq.provisioning.service;

import com.sequenceiq.provisioning.controller.json.StackResult;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.Stack;
import com.sequenceiq.provisioning.domain.StackDescription;
import com.sequenceiq.provisioning.domain.User;

public interface ProvisionService {

    StackResult createStack(User user, Stack stack);

    StackDescription describeStack(User user, Stack stack);

    StackDescription describeStackWithResources(User user, Stack stack);

    void deleteStack(User user, Stack stack);

    CloudPlatform getCloudPlatform();

}
