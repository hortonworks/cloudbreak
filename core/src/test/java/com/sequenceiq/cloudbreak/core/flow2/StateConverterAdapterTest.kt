package com.sequenceiq.cloudbreak.core.flow2

import org.junit.Assert
import org.junit.Test

import com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopState

class StateConverterAdapterTest {
    private val stateConverterAdapter = StateConverterAdapter<StackStopState>(StackStopState::class.java)

    @Test
    fun convertTest() {
        val event = stateConverterAdapter.convert("STOP_STATE")
        Assert.assertEquals(StackStopState.STOP_STATE, event)
    }
}
