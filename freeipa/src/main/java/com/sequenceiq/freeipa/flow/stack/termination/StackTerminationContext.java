package com.sequenceiq.freeipa.flow.stack.termination;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackContext;

public class StackTerminationContext extends StackContext {

    private final List<CloudResource> cloudResources;

    public StackTerminationContext(String flowId, Stack stack, CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack,
            List<CloudResource> cloudResources) {
        super(flowId, stack, cloudContext, cloudCredential, cloudStack);
        this.cloudResources = cloudResources;
    }

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }
}
