package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.domain.Stack;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class StackScalingFlowContext extends StackContext {
    private final Set<String> instanceIds;

    private final String instanceGroupName;

    private final Integer adjustment;

    private final Set<String> hostNames;

    public StackScalingFlowContext(String flowId, Stack stack, CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack,
            String instanceGroupName, Set<String> instanceIds, Integer adjustment) {
        this(flowId, stack, cloudContext, cloudCredential, cloudStack, instanceGroupName, instanceIds, adjustment, Collections.emptySet());
    }

    public StackScalingFlowContext(String flowId, Stack stack, CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack,
            String instanceGroupName, Set<String> instanceIds, Integer adjustment, Set<String> hostNames) {
        super(flowId, stack, cloudContext, cloudCredential, cloudStack);
        this.instanceGroupName = instanceGroupName;
        this.instanceIds = instanceIds;
        this.adjustment = adjustment;
        this.hostNames = hostNames;
    }

    public Collection<String> getInstanceIds() {
        return instanceIds;
    }

    public String getInstanceGroupName() {
        return instanceGroupName;
    }

    public Integer getAdjustment() {
        return adjustment;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }
}
