package com.sequenceiq.cloudbreak.core.flow2.stack;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.cloudbreak.domain.Stack;

public class StackContext extends CommonContext {
    private Stack stack;
    private CloudContext cloudContext;
    private CloudCredential cloudCredential;
    private CloudStack cloudStack;

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
