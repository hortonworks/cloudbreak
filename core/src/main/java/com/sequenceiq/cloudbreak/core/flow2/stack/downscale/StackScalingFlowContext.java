package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.domain.Stack;

public class StackScalingFlowContext extends StackContext {

    private final Set<String> instanceIds;
    private final String instanceGroupName;
    private final Integer adjustment;
    private final ScalingType scalingType;

    public StackScalingFlowContext(String flowId, Stack stack, CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack,
            String instanceGroupName, Set<String> instanceIds, Integer adjustment, ScalingType scalingType) {
        super(flowId, stack, cloudContext, cloudCredential, cloudStack);
        this.instanceGroupName = instanceGroupName;
        this.instanceIds = instanceIds;
        this.adjustment = adjustment;
        this.scalingType = scalingType;
    }

    public Set<String> getInstanceIds() {
        return instanceIds;
    }

    public String getInstanceGroupName() {
        return instanceGroupName;
    }

    public Integer getAdjustment() {
        return adjustment;
    }

    public ScalingType getScalingType() {
        return scalingType;
    }
}
