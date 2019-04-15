package com.sequenceiq.cloudbreak.core.flow2;

import org.junit.Assert;
import org.junit.Test;

public class StateConverterAdapterTest {
    private final StateConverterAdapter<TestState> stateConverterAdapter = new StateConverterAdapter<>(TestState.class);

    @Test
    public void convertTest() {
        TestState state = stateConverterAdapter.convert("TEST_STATE");
        Assert.assertEquals(TestState.TEST_STATE, state);
    }

    private enum TestState implements FlowState {
        TEST_STATE
    }
}
