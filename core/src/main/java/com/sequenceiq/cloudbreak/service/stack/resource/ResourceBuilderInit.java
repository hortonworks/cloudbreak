package com.sequenceiq.cloudbreak.service.stack.resource;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;

public interface ResourceBuilderInit
        <P extends ProvisionContextObject, D extends DeleteContextObject, SSCO extends StartStopContextObject, U extends UpdateContextObject> {

    P provisionInit(Stack stack) throws Exception;

    U updateInit(Stack stack);

    D deleteInit(Stack stack) throws Exception;

    D decommissionInit(Stack stack, Set<String> decommissionSet) throws Exception;

    SSCO startStopInit(Stack stack) throws Exception;

    ResourceBuilderType resourceBuilderType();

    CloudPlatform cloudPlatform();
}
