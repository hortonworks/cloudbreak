package com.sequenceiq.cloudbreak.core.flow2;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopState;

public class StateConverterAdapterTest {
    private StateConverterAdapter<StackStopState> stateConverterAdapter = new StateConverterAdapter<>(StackStopState.class);

    @Test
    public void convertTest() {
        StackStopState event = stateConverterAdapter.convert("STOP_STATE");
        Assert.assertEquals(StackStopState.STOP_STATE, event);
    }
}
