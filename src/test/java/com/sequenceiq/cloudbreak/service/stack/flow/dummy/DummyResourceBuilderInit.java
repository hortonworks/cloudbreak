package com.sequenceiq.cloudbreak.service.stack.flow.dummy;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.resource.DeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.DescribeContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;
import com.sequenceiq.cloudbreak.service.stack.resource.StartStopContextObject;

public class DummyResourceBuilderInit implements ResourceBuilderInit {
    @Override
    public ProvisionContextObject provisionInit(Stack stack, String userData) throws Exception {
        return new DummyProvisionContextObject(1L);
    }

    @Override
    public DeleteContextObject deleteInit(Stack stack) throws Exception {
        return new DummyDeleteContextObject(1L);
    }

    @Override
    public DeleteContextObject decommissionInit(Stack stack, Set decommissionSet) throws Exception {
        return new DummyDeleteContextObject(1L);
    }

    @Override
    public StartStopContextObject startStopInit(Stack stack) throws Exception {
        return new DummyStartStopContextObject(stack);
    }

    @Override
    public DescribeContextObject describeInit(Stack stack) throws Exception {
        return new DummyDescribeContextObject(1L);
    }

    @Override
    public ResourceBuilderType resourceBuilderType() {
        return ResourceBuilderType.RESOURCE_BUILDER_INIT;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.GCC;
    }
}
