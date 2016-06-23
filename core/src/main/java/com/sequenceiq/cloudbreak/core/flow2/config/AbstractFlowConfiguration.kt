package com.sequenceiq.cloudbreak.core.flow2.config

import java.util.ArrayList
import java.util.HashSet

import javax.annotation.PostConstruct
import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.ApplicationContext
import org.springframework.core.task.SyncTaskExecutor
import org.springframework.core.task.TaskExecutor
import org.springframework.messaging.Message
import org.springframework.statemachine.config.ObjectStateMachineFactory
import org.springframework.statemachine.config.StateMachineFactory
import org.springframework.statemachine.config.builders.StateMachineConfigurationBuilder
import org.springframework.statemachine.config.builders.StateMachineStateBuilder
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer
import org.springframework.statemachine.config.builders.StateMachineTransitionBuilder
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer
import org.springframework.statemachine.config.common.annotation.ObjectPostProcessor
import org.springframework.statemachine.config.configurers.ExternalTransitionConfigurer
import org.springframework.statemachine.config.configurers.StateConfigurer
import org.springframework.statemachine.listener.StateMachineListener
import org.springframework.statemachine.listener.StateMachineListenerAdapter
import org.springframework.statemachine.state.State

import com.google.common.base.Optional
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.DefaultFlowTriggerCondition
import com.sequenceiq.cloudbreak.core.flow2.EventConverterAdapter
import com.sequenceiq.cloudbreak.core.flow2.Flow
import com.sequenceiq.cloudbreak.core.flow2.FlowAdapter
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent
import com.sequenceiq.cloudbreak.core.flow2.FlowFinalizeAction
import com.sequenceiq.cloudbreak.core.flow2.FlowState
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggerCondition
import com.sequenceiq.cloudbreak.core.flow2.MessageFactory
import com.sequenceiq.cloudbreak.core.flow2.StateConverterAdapter

abstract class AbstractFlowConfiguration<S : FlowState, E : FlowEvent>(private val stateType: Class<S>, private val eventType: Class<E>) : FlowConfiguration<E> {

    protected var stateMachineFactory: StateMachineFactory<S, E>? = null
        private set

    @Inject
    protected val applicationContext: ApplicationContext? = null

    @PostConstruct
    @Throws(Exception::class)
    fun init() {
        val config = stateMachineConfiguration
        config.configurationBuilder.withConfiguration().listener(config.listener).taskExecutor(config.executor)
        configure(config.stateBuilder, config.transitionBuilder, edgeConfig, transitions)
        stateMachineFactory = ObjectStateMachineFactory(config.configurationBuilder.build(), config.transitionBuilder.build(),
                config.stateBuilder.build())
    }

    override fun createFlow(flowId: String): Flow {
        return FlowAdapter(flowId, stateMachineFactory.getStateMachine(), MessageFactory<E>(), StateConverterAdapter(stateType),
                EventConverterAdapter(eventType), javaClass)
    }

    override val flowTriggerCondition: FlowTriggerCondition
        get() = applicationContext!!.getBean<DefaultFlowTriggerCondition>(DefaultFlowTriggerCondition::class.java)

    @Throws(Exception::class)
    private fun configure(stateConfig: StateMachineStateConfigurer<S, E>, transitionConfig: StateMachineTransitionConfigurer<S, E>,
                          flowEdgeConfig: FlowEdgeConfig<S, E>, transitions: List<Transition<S, E>>) {
        val stateConfigurer = stateConfig.withStates().initial(flowEdgeConfig.initState).end(flowEdgeConfig.finalState)
        var transitionConfigurer: ExternalTransitionConfigurer<S, E>? = null
        val failHandled = HashSet<S>()
        for (transition in transitions) {
            transitionConfigurer = if (transitionConfigurer == null) transitionConfig.withExternal() else transitionConfigurer.and().withExternal()
            val action = getAction(transition.source)
            if (action != null) {
                stateConfigurer.state(transition.source, action, null)
            }
            transitionConfigurer!!.source(transition.source).target(transition.target).event(transition.event)
            if (action != null && transition.failureEvent != null && transition.target !== flowEdgeConfig.defaultFailureState) {
                action.setFailureEvent(transition.failureEvent)
                val failureState = Optional.fromNullable(transition.failureState).or(flowEdgeConfig.defaultFailureState)
                stateConfigurer.state(failureState, getAction(failureState), null)
                transitionConfigurer.and().withExternal().source(transition.source).target(failureState).event(transition.failureEvent)
                if (!failHandled.contains(failureState)) {
                    failHandled.add(failureState)
                    transitionConfigurer.and().withExternal().source(failureState).target(flowEdgeConfig.finalState).event(flowEdgeConfig.failureHandled)
                }
            }
        }
        stateConfigurer.state(flowEdgeConfig.finalState, getAction(FlowFinalizeAction::class.java), null)
    }

    protected val stateMachineConfiguration: MachineConfiguration<S, E>
        get() {
            val configurationBuilder = StateMachineConfigurationBuilder<S, E>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true)
            val stateBuilder = StateMachineStateBuilder<S, E>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true)
            val transitionBuilder = StateMachineTransitionBuilder<S, E>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true)
            val listener = object : StateMachineListenerAdapter<S, E>() {
                override fun stateChanged(from: State<S, E>?, to: State<S, E>?) {
                    LOGGER.info("{} changed from {} to {}", javaClass.getSimpleName(), from, to)
                }

                override fun eventNotAccepted(event: Message<E>?) {
                    LOGGER.error("{} not accepted event: {}", javaClass.getSimpleName(), event)
                }
            }
            return MachineConfiguration(configurationBuilder, stateBuilder, transitionBuilder, listener, SyncTaskExecutor())
        }

    protected abstract val transitions: List<Transition<S, E>>

    protected abstract val edgeConfig: FlowEdgeConfig<S, E>

    private fun getAction(state: FlowState): AbstractAction<FlowState, FlowEvent, CommonContext, Payload>? {
        return if (state.action() == null) getAction(state.name()) else getAction(state.action())
    }

    private fun getAction(clazz: Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>>): AbstractAction<FlowState, FlowEvent, CommonContext, Payload> {
        return applicationContext!!.getBean<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>>(clazz.simpleName, clazz)
    }

    private fun getAction(name: String): AbstractAction<FlowState, FlowEvent, CommonContext, Payload>? {
        try {
            return applicationContext!!.getBean<AbstractAction<FlowState, FlowEvent, CommonContext, Payload>>(name, AbstractAction<FlowState, FlowEvent, CommonContext, Payload>::class.java)
        } catch (ex: NoSuchBeanDefinitionException) {
            return null
        }

    }

    internal class MachineConfiguration<S, E>(private val configurationBuilder: StateMachineConfigurationBuilder<S, E>, private val stateBuilder: StateMachineStateBuilder<S, E>,
                                              private val transitionBuilder: StateMachineTransitionBuilder<S, E>, private val listener: StateMachineListener<S, E>, private val executor: TaskExecutor)

    protected class Transition<S : FlowState, E : FlowEvent> private constructor(private val source: S, private val target: S, private val event: E, private val failureState: S, private val failureEvent: E) {

        class Builder<S : FlowState, E : FlowEvent> {
            private val transitions = ArrayList<Transition<S, E>>()
            private var defaultFailureEvent = Optional.absent<E>()

            fun from(from: S): ToBuilder<S, E> {
                return ToBuilder(from, this)
            }

            fun addTransition(from: S, to: S, with: E, fail: S) {
                if (!defaultFailureEvent.isPresent) {
                    throw UnsupportedOperationException("Default failureEvent event must specified!")
                }
                addTransition(from, to, with, fail, defaultFailureEvent.get())
            }

            fun addTransition(from: S, to: S, with: E, fail: S, withFailure: E) {
                transitions.add(Transition(from, to, with, fail, withFailure))
            }

            fun addTransition(from: S, to: S, with: E) {
                transitions.add(Transition<S, E>(from, to, with, null, null))
            }

            fun build(): List<Transition<S, E>> {
                return transitions
            }

            fun defaultFailureEvent(defaultFailureEvent: E): Builder<S, E> {
                this.defaultFailureEvent = Optional.of(defaultFailureEvent)
                return this
            }
        }

        class ToBuilder<S : FlowState, E : FlowEvent> internal constructor(private val from: S, private val builder: Builder<S, E>) {

            fun to(to: S): WithBuilder<S, E> {
                return WithBuilder(from, to, builder)
            }
        }

        class WithBuilder<S : FlowState, E : FlowEvent> internal constructor(private val from: S, private val to: S, private val builder: Builder<S, E>) {

            fun event(with: E): FailureBuilder<S, E> {
                return FailureBuilder(from, to, with, builder)
            }
        }

        class FailureBuilder<S : FlowState, E : FlowEvent> internal constructor(private val from: S, private val to: S, private val with: E, private val builder: Builder<S, E>) {
            private var failure: S? = null

            fun failureState(toFailure: S): FailureBuilder<S, E> {
                failure = toFailure
                return this
            }

            fun failureEvent(withFailure: E): Builder<S, E> {
                builder.addTransition(from, to, with, failure, withFailure)
                return builder
            }

            fun defaultFailureEvent(): Builder<S, E> {
                builder.addTransition(from, to, with, failure)
                return builder
            }

            fun noFailureEvent(): Builder<S, E> {
                builder.addTransition(from, to, with)
                return builder
            }
        }
    }

    protected class FlowEdgeConfig<S, E>(private val initState: S, private val finalState: S, private val defaultFailureState: S, private val failureHandled: E)

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AbstractFlowConfiguration<FlowState, FlowEvent>::class.java)
    }
}
