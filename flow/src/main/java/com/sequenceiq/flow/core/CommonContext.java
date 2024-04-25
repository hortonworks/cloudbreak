package com.sequenceiq.flow.core;

public class CommonContext {
    private FlowParameters flowParameters;

    public CommonContext(FlowParameters flowParameters) {
        this.flowParameters = flowParameters;
    }

    public String getFlowId() {
        return flowParameters.getFlowId();
    }

    public String getFlowTriggerUserCrn() {
        return flowParameters.getFlowTriggerUserCrn();
    }

    public FlowParameters getFlowParameters() {
        return flowParameters;
    }

    @Override
    public String toString() {
        return "CommonContext{" +
                "flowParameters=" + flowParameters +
                '}';
    }
}
