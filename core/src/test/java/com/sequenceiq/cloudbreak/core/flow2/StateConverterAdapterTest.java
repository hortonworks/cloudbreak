package com.sequenceiq.cloudbreak.core.flow2;

import com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopState;
import org.junit.Assert;
import org.junit.Test;

public class StateConverterAdapterTest {

    private final StateConverterAdapter<StackStopState> stateConverterAdapter = new StateConverterAdapter<>(StackStopState.class);

    @Test
    public void convertTest() {
        StackStopState event = stateConverterAdapter.convert("STOP_STATE");
        Assert.assertEquals(StackStopState.STOP_STATE, event);
    }
}
