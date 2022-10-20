package com.sequenceiq.cloudbreak.core.flow2.cluster;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class ClusterScaleContext extends CommonContext {

    private final Stack stack;

    private final String hostGroupName;

    public ClusterScaleContext(FlowParameters flowParameters, Stack stack, String hostGroupName) {
        super(flowParameters);
        this.stack = stack;
        this.hostGroupName = hostGroupName;
    }

    public Stack getStack() {
        return stack;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }
}
