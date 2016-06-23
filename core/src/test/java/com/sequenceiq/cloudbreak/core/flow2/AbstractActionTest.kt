package com.sequenceiq.cloudbreak.core.flow2

import org.mockito.Matchers.anyString
import org.mockito.Matchers.eq
import org.mockito.Mockito.any
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

import java.util.Collections
import java.util.Optional

import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.springframework.core.task.SyncTaskExecutor
import org.springframework.messaging.support.GenericMessage
import org.springframework.statemachine.StateContext
import org.springframework.statemachine.StateMachine
import org.springframework.statemachine.config.ObjectStateMachineFactory
import org.springframework.statemachine.config.builders.StateMachineConfigurationBuilder
import org.springframework.statemachine.config.builders.StateMachineStateBuilder
import org.springframework.statemachine.config.builders.StateMachineTransitionBuilder
import org.springframework.statemachine.config.common.annotation.ObjectPostProcessor

import com.sequenceiq.cloudbreak.cloud.event.Payload
import com.sequenceiq.cloudbreak.cloud.event.Selectable

import reactor.bus.EventBus

class AbstractActionTest {

    @InjectMocks
    private var underTest: TestAction? = null

    @Mock
    private val eventBus: EventBus? = null

    @Mock
    private val runningFlows: FlowRegister? = null

    @Mock
    private val flow: Flow? = null

    private var stateMachine: StateMachine<State, Event>? = null

    @Before
    @Throws(Exception::class)
    fun setup() {
        underTest = spy(TestAction())
        underTest!!.setFailureEvent(Event.FAILURE)
        MockitoAnnotations.initMocks(this)
        BDDMockito.given(flow!!.flowId).willReturn(FLOW_ID)
        BDDMockito.given(runningFlows!!.get(anyString())).willReturn(flow)
        val configurationBuilder = StateMachineConfigurationBuilder<State, Event>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true)
        configurationBuilder.setTaskExecutor(SyncTaskExecutor())
        val stateBuilder = StateMachineStateBuilder<State, Event>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true)
        stateBuilder.withStates().initial(State.INIT).state(State.DOING, underTest, null)
        val transitionBuilder = StateMachineTransitionBuilder<State, Event>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true)
        transitionBuilder.withExternal().source(State.INIT).target(State.DOING).event(Event.DOIT)
        stateMachine = ObjectStateMachineFactory(configurationBuilder.build(), transitionBuilder.build(), stateBuilder.build()).stateMachine
        stateMachine!!.start()
    }

    @Test
    @Throws(Exception::class)
    fun testExecute() {
        stateMachine!!.sendEvent(GenericMessage(Event.DOIT, Collections.singletonMap<String, Any>("FLOW_ID", FLOW_ID)))
        verify<TestAction>(underTest, times(1)).createFlowContext(eq(FLOW_ID), Matchers.any(StateContext<Any, Any>::class.java), Matchers.any(Payload::class.java))
        verify<TestAction>(underTest, times(1)).doExecute(Matchers.any(CommonContext::class.java), Matchers.any(Payload::class.java), Matchers.any(Map<Any, Any>::class.java))
        verify<TestAction>(underTest, times(0)).sendEvent(Matchers.any(CommonContext::class.java))
        verify<TestAction>(underTest, times(0)).sendEvent(anyString(), anyString(), Matchers.any())
        verify<TestAction>(underTest, times(0)).sendEvent(anyString(), Matchers.any(Selectable::class.java))
        verify<TestAction>(underTest, times(0)).getFailurePayload(Matchers.any(Payload::class.java), Matchers.any(Optional<Any>::class.java), Matchers.any(RuntimeException::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun testFailedExecute() {
        val exception = UnsupportedOperationException()
        Mockito.doThrow(exception).`when`<TestAction>(underTest).doExecute(Matchers.any(CommonContext::class.java), Matchers.any(Payload::class.java), Matchers.any(Map<Any, Any>::class.java))
        stateMachine!!.sendEvent(GenericMessage(Event.DOIT, Collections.singletonMap<String, Any>("FLOW_ID", FLOW_ID)))
        verify<TestAction>(underTest, times(1)).createFlowContext(eq(FLOW_ID), Matchers.any(StateContext<Any, Any>::class.java), Matchers.any(Payload::class.java))
        verify<TestAction>(underTest, times(1)).doExecute(Matchers.any(CommonContext::class.java), Matchers.any(Payload::class.java), Matchers.any(Map<Any, Any>::class.java))
        verify<TestAction>(underTest, times(1)).getFailurePayload(Matchers.any(Payload::class.java), Matchers.any(Optional<Any>::class.java), eq<RuntimeException>(exception))
        verify<TestAction>(underTest, times(1)).sendEvent(eq(FLOW_ID), eq(Event.FAILURE.name), eq<Map<Any, Any>>(emptyMap<Any, Any>()))
    }

    internal enum class State : FlowState {
        INIT, DOING;

        override fun action(): Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>> {
            return TestAction::class.java
        }
    }

    internal enum class Event : FlowEvent {
        DOIT, FAILURE;

        override fun stringRepresentation(): String {
            return name
        }
    }

    internal inner class TestAction protected constructor() : AbstractAction<State, Event, CommonContext, Payload>(Payload::class.java) {

        public override fun createFlowContext(flowId: String, stateContext: StateContext<State, Event>, payload: Payload): CommonContext {
            return CommonContext(FLOW_ID)
        }

        @Throws(Exception::class)
        public override fun doExecute(context: CommonContext, payload: Payload, variables: Map<Any, Any>) {
        }

        override fun createRequest(context: CommonContext): Selectable? {
            return null
        }

        public override fun getFailurePayload(payload: Payload, flowContext: Optional<CommonContext>, ex: Exception): Any {
            return emptyMap<Any, Any>()
        }
    }

    companion object {

        val FLOW_ID = "flowId"
    }
}
