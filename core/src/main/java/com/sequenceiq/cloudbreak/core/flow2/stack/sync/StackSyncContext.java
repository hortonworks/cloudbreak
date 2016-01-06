package com.sequenceiq.cloudbreak.core.flow2.stack.sync;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;

public class StackSyncContext extends CommonContext {

    private Stack stack;
    private List<InstanceMetaData> instanceMetaData;
    private CloudContext cloudContext;
    private CloudCredential cloudCredential;

    public StackSyncContext(String flowId, Stack stack, List<InstanceMetaData> instanceMetaData, CloudContext cloudContext, CloudCredential cloudCredential) {
        super(flowId);
        this.stack = stack;
        this.instanceMetaData = instanceMetaData;
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
    }

    public Stack getStack() {
        return stack;
    }

    public List<InstanceMetaData> getInstanceMetaData() {
        return instanceMetaData;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }
}
