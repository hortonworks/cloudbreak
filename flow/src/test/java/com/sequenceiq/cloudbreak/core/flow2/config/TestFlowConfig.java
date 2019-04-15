package com.sequenceiq.cloudbreak.core.flow2.config;

import java.util.List;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public class TestFlowConfig extends AbstractFlowConfiguration<TestFlowConfig.TestFlowState, TestFlowConfig.TestFlowEvent>
        implements RetryableFlowConfiguration<TestFlowConfig.TestFlowEvent> {
        private static final List<AbstractFlowConfiguration.Transition<TestFlowState, TestFlowEvent>> TRANSITIONS =
                new AbstractFlowConfiguration.Transition.Builder<TestFlowState, TestFlowEvent>()
                        .defaultFailureEvent(TestFlowEvent.TEST_FAILURE_EVENT)
                        .from(TestFlowState.INIT_STATE).to(TestFlowState.TEST_STATE).event(TestFlowEvent.TEST_FLOW_EVENT).noFailureEvent()
                        .from(TestFlowState.TEST_STATE).to(TestFlowState.TEST_FINISHED_STATE).event(TestFlowEvent.TEST_FINISHED_EVENT).defaultFailureEvent()
                        .from(TestFlowState.TEST_FINISHED_STATE).to(TestFlowState.FINAL_STATE).event(TestFlowEvent.TEST_FINALIZED_EVENT).defaultFailureEvent()
                        .build();

        private static final AbstractFlowConfiguration.FlowEdgeConfig<TestFlowConfig.TestFlowState, TestFlowConfig.TestFlowEvent> EDGE_CONFIG =
                new AbstractFlowConfiguration.FlowEdgeConfig<>(TestFlowState.INIT_STATE, TestFlowState.FINAL_STATE, TestFlowState.TEST_FAILED_STATE,
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
        protected List<AbstractFlowConfiguration.Transition<TestFlowState, TestFlowEvent>> getTransitions() {
            return TRANSITIONS;
        }

        @Override
        protected AbstractFlowConfiguration.FlowEdgeConfig<TestFlowState, TestFlowEvent> getEdgeConfig() {
            return EDGE_CONFIG;
        }

    @Override
    public TestFlowEvent getFailHandledEvent() {
        return TestFlowEvent.TEST_FAIL_HANDLED_EVENT;
    }

    public enum TestFlowState implements FlowState {
        INIT_STATE,
        TEST_FAILED_STATE,
        TEST_STATE,
        TEST_FINISHED_STATE,
        FINAL_STATE;
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
