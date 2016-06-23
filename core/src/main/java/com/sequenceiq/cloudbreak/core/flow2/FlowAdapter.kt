package com.sequenceiq.cloudbreak.core.flow2

import org.springframework.statemachine.StateMachine
import org.springframework.statemachine.access.StateMachineAccess
import org.springframework.statemachine.support.DefaultStateMachineContext

import com.sequenceiq.cloudbreak.core.flow2.config.FlowConfiguration

class FlowAdapter<S : FlowState, E>(override val flowId: String, private val flowMachine: StateMachine<S, E>, private val messageFactory: MessageFactory<E>, private val stateConverter: StateConverter<S>,
                                    private val eventConverter: EventConverter<E>, override val flowConfigClass: Class<out FlowConfiguration<FlowEvent>>) : Flow {
    override var isFlowFailed: Boolean = false
        private set(value: Boolean) {
            super.isFlowFailed = value
        }

    override fun initialize() {
        flowMachine.start()
    }

    fun initialize(stateRepresentation: String) {
        val state = stateConverter.convert(stateRepresentation)
        flowMachine.stop()
        val withAllRegions = flowMachine.stateMachineAccessor.withAllRegions()
        for (access in withAllRegions) {
            access.resetStateMachine(DefaultStateMachineContext<S, E>(state, null, null, null))
        }
        flowMachine.start()
    }

    override val currentState: S
        get() = flowMachine.state.id

    override fun sendEvent(key: String, `object`: Any) {
        flowMachine.sendEvent(messageFactory.createMessage(flowId, eventConverter.convert(key), `object`))
    }

    override fun setFlowFailed() {
        this.isFlowFailed = true
    }
}
