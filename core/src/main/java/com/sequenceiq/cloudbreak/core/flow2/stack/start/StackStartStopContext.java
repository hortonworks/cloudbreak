package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

public class StackStartStopContext extends CommonContext {
    private final Stack stack;

    private final List<InstanceMetaData> instanceMetaData;

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    public StackStartStopContext(String flowId, Stack stack, List<InstanceMetaData> instanceMetaData,
            CloudContext cloudContext, CloudCredential cloudCredential) {
        super(flowId);
        this.stack = stack;
        this.instanceMetaData = instanceMetaData;
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
    }

    public Stack getStack() {
        return stack;
    }

    public Iterable<InstanceMetaData> getInstanceMetaData() {
        return instanceMetaData;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }
}
