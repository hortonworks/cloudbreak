package com.sequenceiq.cloudbreak.service.stack.connector;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;

public interface CloudPlatformConnector {

    Set<Resource> buildStack(Stack stack, Map<String, Object> setupProperties);

    Set<Resource> addInstances(Stack stack, Integer instanceCount, String instanceGroup);

    Set<String> removeInstances(Stack stack, Set<String> instanceIds, String instanceGroup);

    void deleteStack(Stack stack, Credential credential);

    void rollback(Stack stack, Set<Resource> resourceSet);

    void startAll(Stack stack);

    void stopAll(Stack stack);

    CloudPlatform getCloudPlatform();

    void updateAllowedSubnets(Stack stack);

    Set<String> getSSHFingerprints(Stack stack, String gateway);

    PlatformParameters getPlatformParameters(Stack stack);

    String checkAndGetPlatformVariant(Stack stack);
}
