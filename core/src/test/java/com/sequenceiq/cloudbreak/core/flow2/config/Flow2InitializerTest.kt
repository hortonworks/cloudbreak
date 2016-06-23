package com.sequenceiq.cloudbreak.core.flow2.config

import org.mockito.BDDMockito.given
import org.mockito.Matchers.any
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

import java.util.ArrayList

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import com.sequenceiq.cloudbreak.core.flow2.Flow2Handler
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncFlowConfig
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFlowConfig

import reactor.bus.EventBus
import reactor.bus.selector.Selector
import reactor.fn.Consumer

class Flow2InitializerTest {

    @InjectMocks
    private var underTest: Flow2Initializer? = null

    @Mock
    private val flowConfigs: List<FlowConfiguration<*>>? = null

    @Mock
    private val reactor: EventBus? = null

    @Mock
    private val flow2Handler: Flow2Handler? = null

    @Before
    fun setUp() {
        underTest = Flow2Initializer()
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testInitialize() {
        val flowConfigs = ArrayList<FlowConfiguration<*>>()
        flowConfigs.add(StackSyncFlowConfig())
        flowConfigs.add(StackTerminationFlowConfig())
        given<Stream<FlowConfiguration<*>>>(this.flowConfigs!!.stream()).willReturn(flowConfigs.stream())
        underTest!!.init()
        verify<EventBus>(reactor, times(1)).on<Event<*>>(any<Selector>(Selector<Any>::class.java), any<Consumer>(Consumer<Any>::class.java))
    }
}
