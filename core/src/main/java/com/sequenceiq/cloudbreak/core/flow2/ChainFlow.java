package com.sequenceiq.cloudbreak.core.flow2;

import com.sequenceiq.cloudbreak.core.flow2.config.FlowConfiguration;

public abstract class ChainFlow implements Flow, Chainable {
    private Flow actFlow;

    public ChainFlow(Flow flow) {
        this.actFlow = flow;
    }

    @Override
    public void initialize() {
        actFlow.initialize();
    }

    @Override
    public FlowState getCurrentState() {
        return actFlow.getCurrentState();
    }

    @Override
    public String getFlowId() {
        return actFlow.getFlowId();
    }

    @Override
    public void setFlowFailed() {
        actFlow.setFlowFailed();
    }

    @Override
    public boolean isFlowFailed() {
        return actFlow.isFlowFailed();
    }

    @Override
    public void sendEvent(String key, Object object) {
        actFlow.sendEvent(key, object);
    }

    @Override
    public Class<? extends FlowConfiguration> getFlowConfigClass() {
        return actFlow.getFlowConfigClass();
    }
}
