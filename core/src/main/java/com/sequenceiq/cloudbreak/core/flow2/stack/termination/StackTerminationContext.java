package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.domain.Stack;

public class StackTerminationContext extends StackContext {

    private List<CloudResource> cloudResources;

    public StackTerminationContext(String flowId, Stack stack, CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack,
            List<CloudResource> cloudResources) {
        super(flowId, stack, cloudContext, cloudCredential, cloudStack);
        this.cloudResources = cloudResources;
    }

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }
}
