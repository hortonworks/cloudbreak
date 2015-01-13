package com.sequenceiq.cloudbreak.service.stack.connector;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;

public interface CloudPlatformConnector {

    void buildStack(Stack stack, String userData, Map<String, Object> setupProperties);

    boolean addInstances(Stack stack, String userData, Integer instanceCount);

    boolean removeInstances(Stack stack, Set<String> instanceIds);

    void deleteStack(Stack stack, Credential credential);

    void rollback(Stack stack, Set<Resource> resourceSet);

    boolean startAll(Stack stack);

    boolean stopAll(Stack stack);

    CloudPlatform getCloudPlatform();

}
