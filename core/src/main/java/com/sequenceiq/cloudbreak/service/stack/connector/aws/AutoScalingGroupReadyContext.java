package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class AutoScalingGroupReadyContext extends StackContext {

    private String autoScalingGroupName;
    private Integer requiredInstances;

    public AutoScalingGroupReadyContext(Stack stack, String asGroupName, Integer requiredInstances) {
        super(stack);
        this.autoScalingGroupName = asGroupName;
        this.requiredInstances = requiredInstances;
    }

    public String getAutoScalingGroupName() {
        return autoScalingGroupName;
    }

    public Integer getRequiredInstances() {
        return requiredInstances;
    }

}
