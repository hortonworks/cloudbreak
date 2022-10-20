package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class StackStartStopContext extends CommonContext {
    private final StackDto stack;

    private final List<InstanceGroupDto> instanceGroupDtos;

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    public StackStartStopContext(FlowParameters flowParameters, StackDto stack, List<InstanceGroupDto> instanceGroupDtos,
        CloudContext cloudContext, CloudCredential cloudCredential) {
        super(flowParameters);
        this.stack = stack;
        this.instanceGroupDtos = instanceGroupDtos;
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
    }

    public StackDto getStack() {
        return stack;
    }

    public List<InstanceGroupDto> getInstanceGroupDtos() {
        return instanceGroupDtos;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }
}
