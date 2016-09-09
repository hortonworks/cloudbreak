package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;

public class InstanceTerminationContext extends CommonContext {

    private final Stack stack;
    private final CloudContext cloudContext;
    private final CloudCredential cloudCredential;
    private final CloudStack cloudStack;
    private final List<CloudResource> cloudResources;
    private final CloudInstance cloudInstance;
    private final InstanceMetaData instanceMetaData;

    public InstanceTerminationContext(String flowId, Stack stack, CloudContext cloudContext, CloudCredential cloudCredential,
            CloudStack cloudStack, List<CloudResource> cloudResources, CloudInstance cloudInstance, InstanceMetaData instanceMetaData) {
        super(flowId);
        this.stack = stack;
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.cloudStack = cloudStack;
        this.cloudResources = ImmutableList.copyOf(cloudResources);
        this.cloudInstance = cloudInstance;
        this.instanceMetaData = instanceMetaData;
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

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }

    public CloudInstance getCloudInstance() {
        return cloudInstance;
    }

    public InstanceMetaData getInstanceMetaData() {
        return instanceMetaData;
    }
}
