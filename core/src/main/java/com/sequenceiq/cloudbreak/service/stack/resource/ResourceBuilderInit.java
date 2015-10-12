package com.sequenceiq.cloudbreak.service.stack.resource;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;

// TODO Have to be removed when the termination of the old version of azure clusters won't be supported anymore
public interface ResourceBuilderInit<D extends DeleteContextObject> {
    D deleteInit(Stack stack) throws Exception;

    CloudPlatform cloudPlatform();
}
