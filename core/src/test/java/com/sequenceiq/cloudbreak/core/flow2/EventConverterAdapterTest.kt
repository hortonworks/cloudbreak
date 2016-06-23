package com.sequenceiq.cloudbreak.core.flow2

import org.junit.Assert
import org.junit.Test

import com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopEvent

class EventConverterAdapterTest {
    private val eventConverter = EventConverterAdapter<StackStopEvent>(StackStopEvent::class.java)

    @Test
    fun convertTest() {
        val event = eventConverter.convert("STOPSTACKFINALIZED")
        Assert.assertEquals(StackStopEvent.STOP_FINALIZED_EVENT, event)
    }
}
