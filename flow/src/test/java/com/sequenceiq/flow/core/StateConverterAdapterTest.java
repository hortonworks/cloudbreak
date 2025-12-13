package com.sequenceiq.flow.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.flow.core.restart.DefaultRestartAction;

class StateConverterAdapterTest {
    private final StateConverterAdapter<TestState> stateConverterAdapter = new StateConverterAdapter<>(TestState.class);

    @Test
    void convertTest() {
        TestState state = stateConverterAdapter.convert("TEST_STATE");
        assertEquals(TestState.TEST_STATE, state);
    }

    private enum TestState implements FlowState {
        TEST_STATE;

        @Override
        public Class<? extends RestartAction> restartAction() {
            return DefaultRestartAction.class;
        }
    }
}
