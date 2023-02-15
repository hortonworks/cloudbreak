package com.sequenceiq.cloudbreak.core.flow2.stack.clusterproxy.reregister;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class ClusterProxyReRegistrationContext extends CommonContext {
    private final Stack stack;

    private final String originalCrn;

    public ClusterProxyReRegistrationContext(FlowParameters flowParameters, Stack stack, String originalCrn) {
        super(flowParameters);
        this.stack = stack;
        this.originalCrn = originalCrn;
    }

    public Stack getStack() {
        return stack;
    }

    public String getOriginalCrn() {
        return originalCrn;
    }
}
