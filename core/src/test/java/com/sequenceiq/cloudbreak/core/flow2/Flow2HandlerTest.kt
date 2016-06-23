package com.sequenceiq.cloudbreak.core.flow2

import org.mockito.BDDMockito.given
import org.mockito.Matchers.any
import org.mockito.Matchers.anyLong
import org.mockito.Matchers.anyString
import org.mockito.Matchers.eq
import org.mockito.Matchers.isNull
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

import java.util.Collections
import java.util.HashMap

import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito
import org.mockito.InjectMocks
import org.mockito.Matchers
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import com.sequenceiq.cloudbreak.cloud.event.Payload
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChains
import com.sequenceiq.cloudbreak.core.flow2.config.FlowConfiguration
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogService

import reactor.bus.Event

class Flow2HandlerTest {

    @InjectMocks
    private var underTest: Flow2Handler? = null

    @Mock
    private val flowLogService: FlowLogService? = null

    @Mock
    private val flowConfigurationMap: Map<String, FlowConfiguration<*>>? = null

    @Mock
    private val runningFlows: FlowRegister? = null

    @Mock
    private val flowConfig: FlowConfiguration<FlowEvent>? = null

    @Mock
    private val flowChains: FlowChains? = null

    @Mock
    private val flowTriggerCondition: FlowTriggerCondition? = null

    @Mock
    private val flow: Flow? = null

    private var flowState: FlowState? = null
    private var dummyEvent: Event<out Payload>? = null

    private val payload = { 1L }

    @Before
    fun setUp() {
        underTest = Flow2Handler()
        MockitoAnnotations.initMocks(this)
        val headers = HashMap<String, Any>()
        headers.put("FLOW_ID", FLOW_ID)
        dummyEvent = Event<Payload>(Event.Headers(headers), payload)
        flowState = OwnFlowState()
    }

    @Test
    fun testNewFlow() {
        BDDMockito.given<FlowConfiguration<FlowEvent>>(flowConfigurationMap!!.get(any<Any>())).willReturn(flowConfig)
        given(flowConfig!!.createFlow(anyString())).willReturn(flow)
        given(flowConfig.flowTriggerCondition).willReturn(flowTriggerCondition)
        given(flowTriggerCondition!!.isFlowTriggerable(anyLong())).willReturn(true)
        given(flow!!.currentState).willReturn(flowState)
        val event = Event<Payload>(payload)
        event.key = "KEY"
        underTest!!.accept(event)
        verify(flowConfigurationMap, times(1))[anyString()]
        verify<FlowRegister>(runningFlows, times(1)).put(eq(flow), isNull<String>(String::class.java))
        verify<FlowLogService>(flowLogService, times(1)).save(anyString(), eq("KEY"), any<Payload>(Payload::class.java), eq<Class<out FlowConfiguration>>(flowConfig.javaClass), eq<FlowState>(flowState))
        verify(flow, times(1)).sendEvent(anyString(), any<Any>())
    }

    @Test
    fun testNewFlowButNotHandled() {
        val event = Event<Payload>(payload)
        event.key = "KEY"
        underTest!!.accept(event)
        verify<Map<String, FlowConfiguration<*>>>(flowConfigurationMap, times(1))[anyString()]
        verify<FlowRegister>(runningFlows, times(0)).put(any<Flow>(Flow::class.java), isNull<String>(String::class.java))
        verify<FlowLogService>(flowLogService, times(0)).save(anyString(), anyString(), any<Payload>(Payload::class.java), Matchers.any<Class<Any>>(), any<FlowState>(FlowState::class.java))
    }

    @Test
    fun testExistingFlow() {
        BDDMockito.given<FlowConfiguration<FlowEvent>>(flowConfigurationMap!!.get(any<Any>())).willReturn(flowConfig)
        given(runningFlows!!.get(anyString())).willReturn(flow)
        given(flow!!.currentState).willReturn(flowState)
        dummyEvent!!.key = "KEY"
        underTest!!.accept(dummyEvent)
        verify<FlowLogService>(flowLogService, times(1)).save(eq(FLOW_ID), eq("KEY"), any<Payload>(Payload::class.java), any<Class<Any>>(Class<Any>::class.java), eq<FlowState>(flowState))
        verify(flow, times(1)).sendEvent(eq("KEY"), any<Any>())
    }

    @Test
    fun testExistingFlowNotFound() {
        BDDMockito.given<FlowConfiguration<FlowEvent>>(flowConfigurationMap!!.get(any<Any>())).willReturn(flowConfig)
        dummyEvent!!.key = "KEY"
        underTest!!.accept(dummyEvent)
        verify<FlowLogService>(flowLogService, times(0)).save(anyString(), anyString(), any<Payload>(Payload::class.java), Matchers.any<Class<Any>>(), any<FlowState>(FlowState::class.java))
        verify<Flow>(flow, times(0)).sendEvent(anyString(), any<Any>())
    }

    @Test
    fun testFlowFinalFlowNotChained() {
        given(runningFlows!!.remove(FLOW_ID)).willReturn(flow)
        dummyEvent!!.key = Flow2Handler.FLOW_FINAL
        underTest!!.accept(dummyEvent)
        verify<FlowLogService>(flowLogService, times(1)).close(anyLong(), eq(FLOW_ID))
        verify(runningFlows, times(1)).remove(eq(FLOW_ID))
        verify(runningFlows, times(0)).get(eq(FLOW_ID))
        verify(runningFlows, times(0)).put(any<Flow>(Flow::class.java), isNull<String>(String::class.java))
        verify<FlowChains>(flowChains, times(0)).removeFlowChain(anyString())
        verify<FlowChains>(flowChains, times(0)).triggerNextFlow(anyString())
    }

    @Test
    fun testFlowFinalFlowChained() {
        given(runningFlows!!.remove(FLOW_ID)).willReturn(flow)
        dummyEvent!!.key = Flow2Handler.FLOW_FINAL
        dummyEvent!!.headers.set("FLOW_CHAIN_ID", FLOW_CHAIN_ID)
        underTest!!.accept(dummyEvent)
        verify<FlowLogService>(flowLogService, times(1)).close(anyLong(), eq(FLOW_ID))
        verify(runningFlows, times(1)).remove(eq(FLOW_ID))
        verify(runningFlows, times(0)).get(eq(FLOW_ID))
        verify(runningFlows, times(0)).put(any<Flow>(Flow::class.java), isNull<String>(String::class.java))
        verify<FlowChains>(flowChains, times(0)).removeFlowChain(anyString())
        verify<FlowChains>(flowChains, times(1)).triggerNextFlow(eq(FLOW_CHAIN_ID))
    }

    @Test
    fun testFlowFinalFlowFailed() {
        given(flow!!.isFlowFailed).willReturn(java.lang.Boolean.TRUE)
        given(runningFlows!!.remove(FLOW_ID)).willReturn(flow)
        dummyEvent!!.key = Flow2Handler.FLOW_FINAL
        given(runningFlows.remove(anyString())).willReturn(flow)
        underTest!!.accept(dummyEvent)
        verify<FlowLogService>(flowLogService, times(1)).close(anyLong(), eq(FLOW_ID))
        verify(runningFlows, times(1)).remove(eq(FLOW_ID))
        verify(runningFlows, times(0)).get(eq(FLOW_ID))
        verify(runningFlows, times(0)).put(any<Flow>(Flow::class.java), isNull<String>(String::class.java))
        verify<FlowChains>(flowChains, times(1)).removeFlowChain(anyString())
        verify<FlowChains>(flowChains, times(0)).triggerNextFlow(anyString())
    }

    @Test
    fun testCancelRunningFlows() {
        given(flowLogService!!.findAllRunningNonTerminationFlowIdsByStackId(anyLong())).willReturn(setOf<String>(FLOW_ID))
        given(runningFlows!!.remove(FLOW_ID)).willReturn(flow)
        given(runningFlows.getFlowChainId(eq(FLOW_ID))).willReturn(FLOW_CHAIN_ID)
        dummyEvent!!.key = Flow2Handler.FLOW_CANCEL
        underTest!!.accept(dummyEvent)
        verify(flowLogService, times(1)).cancel(anyLong(), eq(FLOW_ID))
        verify<FlowChains>(flowChains, times(1)).removeFlowChain(eq(FLOW_CHAIN_ID))
    }

    private class OwnFlowState : FlowState {
        override fun action(): Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>>? {
            return null
        }

        override fun name(): String? {
            return null
        }
    }

    companion object {

        val FLOW_ID = "flowId"
        val FLOW_CHAIN_ID = "flowChainId"
    }
}
