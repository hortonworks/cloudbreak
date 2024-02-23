package com.sequenceiq.cloudbreak.core.flow2.cluster.refreshentitlement;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class RefreshEntitlementParamsContext extends CommonContext {

    private final StackDto stack;

    public RefreshEntitlementParamsContext(FlowParameters flowParameters, StackDto stack) {
        super(flowParameters);
        this.stack = stack;
    }

    public StackDto getStack() {
        return stack;
    }

}

