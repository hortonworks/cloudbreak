package com.sequenceiq.cloudbreak.core.flow2;

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
    public void sendEvent(String key, Object object) {
        actFlow.sendEvent(key, object);
    }
}
