package com.sequenceiq.cloudbreak.core.flow2.cluster;

import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.cloudbreak.domain.Stack;

public class ClusterScaleContext extends CommonContext {

    private final Stack stack;
    private final String hostGroupName;

    public ClusterScaleContext(String flowId, Stack stack, String hostGroupName) {
        super(flowId);
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
