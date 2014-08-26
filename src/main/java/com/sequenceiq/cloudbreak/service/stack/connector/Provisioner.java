package com.sequenceiq.cloudbreak.service.stack.connector;

import java.util.Map;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;

public interface Provisioner {

    void buildStack(Stack stack, String userData, Map<String, Object> setupProperties);

    void addNode(Stack stack, String userData, Integer nodeCount);

    CloudPlatform getCloudPlatform();

}
