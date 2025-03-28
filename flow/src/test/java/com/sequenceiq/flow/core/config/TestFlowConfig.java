package com.sequenceiq.flow.core.config;

import java.util.List;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.TestFlowConfig.TestFlowEvent;
import com.sequenceiq.flow.core.config.TestFlowConfig.TestFlowState;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public class TestFlowConfig extends AbstractFlowConfiguration<TestFlowState, TestFlowEvent>
        implements RetryableFlowConfiguration<TestFlowEvent> {
        private static final List<Transition<TestFlowState, TestFlowEvent>> TRANSITIONS =
                new Builder<TestFlowState, TestFlowEvent>()
                        .defaultFailureEvent(TestFlowEvent.TEST_FAILURE_EVENT)
                        .from(TestFlowState.INIT_STATE).to(TestFlowState.TEST_STATE).event(TestFlowEvent.TEST_FLOW_EVENT).noFailureEvent()
                        .from(TestFlowState.TEST_STATE).to(TestFlowState.TEST_FINISHED_STATE).event(TestFlowEvent.TEST_FINISHED_EVENT).defaultFailureEvent()
                        .from(TestFlowState.TEST_FINISHED_STATE).to(TestFlowState.FINAL_STATE).event(TestFlowEvent.TEST_FINALIZED_EVENT).defaultFailureEvent()
                        .build();

        private static final FlowEdgeConfig<TestFlowState, TestFlowEvent> EDGE_CONFIG =
                new FlowEdgeConfig<>(TestFlowState.INIT_STATE, TestFlowState.FINAL_STATE, TestFlowState.TEST_FAILED_STATE,
                        TestFlowEvent.TEST_FAIL_HANDLED_EVENT);

    public TestFlowConfig() {
            super(TestFlowState.class, TestFlowEvent.class);
        }

        @Override
        public TestFlowEvent[] getEvents() {
            return TestFlowEvent.values();
        }

        @Override
        public TestFlowEvent[] getInitEvents() {
            return new TestFlowEvent[] {
                    TestFlowEvent.TEST_FLOW_EVENT
            };
        }

    @Override
    public String getDisplayName() {
        return "Test flow config";
    }

    @Override
    protected List<Transition<TestFlowState, TestFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<TestFlowState, TestFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public TestFlowEvent getRetryableEvent() {
        return TestFlowEvent.TEST_FAIL_HANDLED_EVENT;
    }

    public enum TestFlowState implements FlowState {
        INIT_STATE,
        TEST_FAILED_STATE,
        TEST_STATE,
        TEST_FINISHED_STATE,
        FINAL_STATE;

        @Override
        public Class<? extends RestartAction> restartAction() {
            return DefaultRestartAction.class;
        }
    }

    public enum TestFlowEvent implements FlowEvent {
        TEST_FLOW_EVENT("TestFlowEvent"),
        TEST_FINISHED_EVENT("TestFinishedEvent"),
        TEST_FAILURE_EVENT("TestFailureEvent"),
        TEST_FINALIZED_EVENT("TESTFINALIZED"),
        TEST_FAIL_HANDLED_EVENT("TESTFAILHANDLED");

        private final String event;

        TestFlowEvent(String event) {
            this.event = event;
        }

        @Override
        public String event() {
            return event;
        }
    }
}
