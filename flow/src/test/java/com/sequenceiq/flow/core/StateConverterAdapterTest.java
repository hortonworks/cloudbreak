package com.sequenceiq.flow.core;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public class StateConverterAdapterTest {
    private final StateConverterAdapter<TestState> stateConverterAdapter = new StateConverterAdapter<>(TestState.class);

    @Test
    public void convertTest() {
        TestState state = stateConverterAdapter.convert("TEST_STATE");
        Assert.assertEquals(TestState.TEST_STATE, state);
    }

    private enum TestState implements FlowState {
        TEST_STATE;

        @Override
        public Class<? extends RestartAction> restartAction() {
            return DefaultRestartAction.class;
        }
    }
}
