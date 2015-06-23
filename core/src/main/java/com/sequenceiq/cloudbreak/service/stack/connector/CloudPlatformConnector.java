package com.sequenceiq.cloudbreak.service.stack.connector;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;

public interface CloudPlatformConnector {

    Set<Resource> buildStack(Stack stack, String gateWayUserData, String coreUserData, Map<String, Object> setupProperties);

    Set<Resource> addInstances(Stack stack, String gateWayUserData, String coreUserData, Integer instanceCount, String instanceGroup);

    Set<String> removeInstances(Stack stack, Set<String> instanceIds, String instanceGroup);

    void deleteStack(Stack stack, Credential credential);

    void rollback(Stack stack, Set<Resource> resourceSet);

    void startAll(Stack stack);

    void stopAll(Stack stack);

    CloudPlatform getCloudPlatform();

    void updateAllowedSubnets(Stack stack, String gateWayUserData, String coreUserData);

    String getSSHUser();

    String getSSHFingerprint(Stack stack, String gateway);

}
