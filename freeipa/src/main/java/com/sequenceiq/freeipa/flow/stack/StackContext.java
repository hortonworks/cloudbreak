package com.sequenceiq.freeipa.flow.stack;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.freeipa.entity.Stack;

public class StackContext extends CommonContext {

    private final Stack stack;

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final CloudStack cloudStack;

    public StackContext(String flowId, Stack stack, CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack) {
        super(flowId);
        this.stack = stack;
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.cloudStack = cloudStack;
    }

    public Stack getStack() {
        return stack;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public CloudStack getCloudStack() {
        return cloudStack;
    }

}
