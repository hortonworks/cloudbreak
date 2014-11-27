package com.sequenceiq.cloudbreak.service.stack.resource;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;

public interface ResourceBuilderInit
        <P extends ProvisionContextObject, D extends DeleteContextObject, DCO extends DescribeContextObject, SSCO extends StartStopContextObject> {

    P provisionInit(Stack stack, String userData) throws Exception;

    D deleteInit(Stack stack) throws Exception;

    D decommissionInit(Stack stack, Set<String> decommisionSet) throws Exception;

    SSCO startStopInit(Stack stack) throws Exception;

    DCO describeInit(Stack stack) throws Exception;

    ResourceBuilderType resourceBuilderType();

    CloudPlatform cloudPlatform();
}
