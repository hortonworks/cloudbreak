package com.sequenceiq.freeipa.flow.stack.start;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

public class StackStartContext extends CommonContext {
    private final Stack stack;

    private final Set<InstanceMetaData> instanceMetaData;

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    public StackStartContext(FlowParameters flowParameters, Stack stack, Set<InstanceMetaData> instanceMetaData,
            CloudContext cloudContext, CloudCredential cloudCredential) {
        super(flowParameters);
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
