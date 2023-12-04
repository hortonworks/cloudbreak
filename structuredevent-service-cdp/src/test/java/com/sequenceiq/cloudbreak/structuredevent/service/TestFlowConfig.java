package com.sequenceiq.cloudbreak.structuredevent.service;

import java.util.List;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

public class TestFlowConfig extends AbstractFlowConfiguration<TestFlowState, TestEvent> {

    public TestFlowConfig(Class stateType, Class eventType) {
        super(stateType, eventType);
    }

    @Override
    protected List<Transition<TestFlowState, TestEvent>> getTransitions() {
        return null;
    }

    @Override
    public FlowEdgeConfig getEdgeConfig() {
        return new FlowEdgeConfig(TestFlowState.INIT_STATE, TestFlowState.FINAL_STATE, TestFlowState.FAILED_STATE, TestEvent.FAIL_HANDLED_EVENT);
    }

    @Override
    public TestEvent[] getEvents() {
        return new TestEvent[0];
    }

    @Override
    public TestEvent[] getInitEvents() {
        return new TestEvent[0];
    }

    @Override
    public String getDisplayName() {
        return null;
    }
}
