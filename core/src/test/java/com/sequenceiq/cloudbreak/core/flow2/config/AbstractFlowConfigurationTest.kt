package com.sequenceiq.cloudbreak.core.flow2.config

import org.junit.Assert.assertEquals
import org.mockito.BDDMockito.given
import org.mockito.Matchers.any
import org.mockito.Matchers.anyString
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.context.ApplicationContext
import org.springframework.core.task.SyncTaskExecutor
import org.springframework.messaging.Message
import org.springframework.statemachine.config.builders.StateMachineConfigurationBuilder
import org.springframework.statemachine.config.builders.StateMachineStateBuilder
import org.springframework.statemachine.config.builders.StateMachineTransitionBuilder
import org.springframework.statemachine.config.common.annotation.ObjectPostProcessor
import org.springframework.statemachine.listener.StateMachineListener
import org.springframework.statemachine.listener.StateMachineListenerAdapter

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.Flow
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent
import com.sequenceiq.cloudbreak.core.flow2.FlowFinalizeAction
import com.sequenceiq.cloudbreak.core.flow2.FlowState

class AbstractFlowConfigurationTest {

    @InjectMocks
    private var underTest: FlowConfiguration? = null

    @Mock
    private val applicationContext: ApplicationContext? = null

    @Mock
    private val action: AbstractAction<State, Event, *, *>? = null

    private var flow: Flow? = null
    private var transitions: List<FlowConfiguration.Transition<State, Event>>? = null
    private var edgeConfig: FlowConfiguration.FlowEdgeConfig<State, Event>? = null

    @Before
    @Throws(Exception::class)
    fun setup() {
        underTest = FlowConfiguration()
        MockitoAnnotations.initMocks(this)
        given(applicationContext!!.getBean<Any>(anyString(), any<Class>(Class<Any>::class.java))).willReturn(action)
        transitions = AbstractFlowConfiguration.Transition.Builder<State, Event>().defaultFailureEvent(Event.FAILURE).from(State.INIT).to(State.DO).event(Event.START).noFailureEvent().from(State.DO).to(State.DO2).event(Event.CONTINUE).defaultFailureEvent().from(State.DO2).to(State.FINISH).event(Event.FINISHED).failureState(State.FAILED2).failureEvent(Event.FAILURE2).from(State.FINISH).to(State.FINAL).event(Event.FINALIZED).defaultFailureEvent().build()
        edgeConfig = FlowConfiguration.FlowEdgeConfig(State.INIT, State.FINAL, State.FAILED, Event.FAIL_HANDLED)
        underTest!!.init()
        verify(applicationContext, times(8)).getBean<Any>(anyString(), any<Class>(Class<Any>::class.java))
        flow = underTest!!.createFlow("flowId")
        flow!!.initialize()
    }

    @Test
    fun testHappyFlowConfiguration() {
        flow!!.sendEvent(Event.START.name, null)
        flow!!.sendEvent(Event.CONTINUE.name, null)
        flow!!.sendEvent(Event.FINISHED.name, null)
        flow!!.sendEvent(Event.FINALIZED.name, null)
    }

    @Test
    fun testUnhappyFlowConfigurationWithDefaultFailureHandler() {
        flow!!.sendEvent(Event.START.name, null)
        flow!!.sendEvent(Event.FAILURE.name, null)
        flow!!.sendEvent(Event.FAIL_HANDLED.name, null)
    }

    @Test
    fun testUnhappyFlowConfigurationWithCustomFailureHandler() {
        flow!!.sendEvent(Event.START.name, null)
        flow!!.sendEvent(Event.CONTINUE.name, null)
        flow!!.sendEvent(Event.FAILURE2.name, null)
        assertEquals("Must be on the FAILED2 state", State.FAILED2, flow!!.currentState)
        flow!!.sendEvent(Event.FAIL_HANDLED.name, null)
    }

    @Test(expected = FlowConfiguration.NotAcceptedException::class)
    fun testUnacceptedFlowConfiguration1() {
        flow!!.sendEvent(Event.START.name, null)
        flow!!.sendEvent(Event.FINISHED.name, null)
    }

    @Test(expected = FlowConfiguration.NotAcceptedException::class)
    fun testUnacceptedFlowConfiguration2() {
        flow!!.sendEvent(Event.START.name, null)
        flow!!.sendEvent(Event.FAILURE2.name, null)
    }

    @Test(expected = FlowConfiguration.NotAcceptedException::class)
    fun testUnacceptedFlowConfiguration3() {
        flow!!.sendEvent(Event.START.name, null)
        flow!!.sendEvent(Event.CONTINUE.name, null)
        flow!!.sendEvent(Event.FAIL_HANDLED.name, null)
    }

    @Test(expected = FlowConfiguration.NotAcceptedException::class)
    fun testUnacceptedFlowConfiguration4() {
        flow!!.sendEvent(Event.START.name, null)
        flow!!.sendEvent(Event.CONTINUE.name, null)
        flow!!.sendEvent(Event.FAILURE.name, null)
    }

    internal enum class State : FlowState {
        INIT, DO, DO2, FINISH, FAILED, FAILED2, FINAL;

        override fun action(): Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>> {
            return FlowFinalizeAction::class.java
        }
    }

    internal enum class Event : FlowEvent {
        START, CONTINUE, FINISHED, FAILURE, FAILURE2, FINALIZED, FAIL_HANDLED;

        override fun stringRepresentation(): String {
            return name
        }
    }

    internal inner class FlowConfiguration private constructor() : AbstractFlowConfiguration<State, Event>(State::class.java, Event::class.java) {

        protected override val stateMachineConfiguration: FlowConfiguration.MachineConfiguration<State, Event>
            get() {
                val configurationBuilder = StateMachineConfigurationBuilder<State, Event>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true)
                val stateBuilder = StateMachineStateBuilder<State, Event>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true)
                val transitionBuilder = StateMachineTransitionBuilder<State, Event>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true)
                val listener = object : StateMachineListenerAdapter<State, Event>() {
                    override fun eventNotAccepted(event: Message<Event>?) {
                        throw NotAcceptedException()
                    }
                }
                return FlowConfiguration.MachineConfiguration(configurationBuilder, stateBuilder, transitionBuilder, listener, SyncTaskExecutor())
            }

        protected override val transitions: List<AbstractFlowConfiguration.Transition<State, Event>>
            get() = transitions

        protected override val edgeConfig: AbstractFlowConfiguration.FlowEdgeConfig<State, Event>
            get() = edgeConfig

        override val events: Array<Event>
            get() = arrayOfNulls(0)

        override val initEvents: Array<Event>
            get() = arrayOf(Event.START)

        internal inner class NotAcceptedException : RuntimeException()
    }
}
