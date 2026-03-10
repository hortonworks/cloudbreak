package com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams;

import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class ResetJvmParamsContext extends CommonContext {

    private final Long stackId;

    public ResetJvmParamsContext(FlowParameters flowParameters, Long stackId) {
        super(flowParameters);
        this.stackId = stackId;
    }

    public Long getStackId() {
        return stackId;
    }
}
