package com.sequenceiq.cloudbreak.service.stack.connector;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;

public interface Provisioner {

    void buildStack(Stack stack, String userData, Map<String, Object> setupProperties);

    void addInstances(Stack stack, String userData, Integer instanceCount);

    void removeInstances(Stack stack, Set<String> instanceIds);

    CloudPlatform getCloudPlatform();

}
