package com.sequenceiq.cloudbreak.core.flow2.config;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.ObjectStateMachineFactory;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.config.builders.StateMachineConfigurationBuilder;
import org.springframework.statemachine.config.builders.StateMachineStateBuilder;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionBuilder;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.config.common.annotation.ObjectPostProcessor;
import org.springframework.statemachine.config.configurers.ExternalTransitionConfigurer;
import org.springframework.statemachine.config.configurers.StateConfigurer;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowFinalizeAction;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public abstract class AbstractFlowConfiguration<S extends FlowState, E extends FlowEvent> implements FlowConfiguration<S, E> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFlowConfiguration.class);

    private StateMachineFactory<S, E> stateMachineFactory;

    @Inject
    private ApplicationContext applicationContext;

    @PostConstruct
    public void init() throws Exception {
        MachineConfiguration<S, E> config = getStateMachineConfiguration();
        config.configurationBuilder.withConfiguration().listener(config.listener).taskExecutor(config.executor);
        configure(config.stateBuilder, config.transitionBuilder, getEdgeConfig(), getTransitions());
        stateMachineFactory = new ObjectStateMachineFactory<>(config.configurationBuilder.build(), config.transitionBuilder.build(),
                config.stateBuilder.build());
    }

    protected StateMachineFactory<S, E> getStateMachineFactory() {
        return stateMachineFactory;
    }

    private void configure(StateMachineStateConfigurer<S, E> stateConfig, StateMachineTransitionConfigurer<S, E> transitionConfig,
            FlowEdgeConfig<S, E> flowEdgeConfig, List<Transition<S, E>> transitions) throws Exception {
        StateConfigurer<S, E> stateConfigurer = stateConfig.withStates()
                .initial(flowEdgeConfig.initState, flowEdgeConfig.initState.action() != null ? getAction(flowEdgeConfig.initState.action()) : null)
                .end(flowEdgeConfig.finalState);
        ExternalTransitionConfigurer<S, E> transitionConfigurer = null;
        for (Transition<S, E> transition : transitions) {
            transitionConfigurer = transitionConfigurer == null ? transitionConfig.withExternal() : transitionConfigurer.and().withExternal();
            stateConfigurer.state(transition.target, getAction(transition.target.action()), null);
            transitionConfigurer.source(transition.source).target(transition.target).event(transition.event);
            if (transition.target.failureEvent() != null && transition.target != flowEdgeConfig.defaultFailureState) {
                S failureState = Optional.fromNullable((S) transition.target.failureState()).or(flowEdgeConfig.defaultFailureState);
                stateConfigurer.state(failureState, getAction(failureState.action()), null);
                transitionConfigurer.and().withExternal().source(transition.target).target(failureState).event((E) transition.target.failureEvent());
                transitionConfigurer.and().withExternal().source(failureState).target(flowEdgeConfig.finalState).event(flowEdgeConfig.failureHandled);
            }
        }
        stateConfigurer.state(flowEdgeConfig.finalState, getAction(FlowFinalizeAction.class), null);
        transitionConfigurer.and().withExternal().source(flowEdgeConfig.lastState).target(flowEdgeConfig.finalState).event(flowEdgeConfig.lastEvent);
    }

    protected MachineConfiguration<S, E> getStateMachineConfiguration() {
        StateMachineConfigurationBuilder<S, E> configurationBuilder =
                new StateMachineConfigurationBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineStateBuilder<S, E> stateBuilder =
                new StateMachineStateBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineTransitionBuilder<S, E> transitionBuilder =
                new StateMachineTransitionBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineListener<S, E> listener =
                new StateMachineListenerAdapter<S, E>() {
                    @Override
                    public void stateChanged(State<S, E> from, State<S, E> to) {
                        LOGGER.info("{} changed from {} to {}", getClass().getSimpleName(), from, to);
                    }

                    @Override
                    public void eventNotAccepted(Message<E> event) {
                        LOGGER.error("{} not accepted event: {}", getClass().getSimpleName(), event.getClass().getSimpleName());
                    }
                };
        return new MachineConfiguration<>(configurationBuilder, stateBuilder, transitionBuilder, listener, new SyncTaskExecutor());
    }

    private Action<S, E> getAction(Class<? extends Action> clazz) {
        return applicationContext.getBean(clazz.getSimpleName(), clazz);
    }

    public abstract Flow<S, E> createFlow(String flowId);

    protected abstract List<Transition<S, E>> getTransitions();

    protected abstract FlowEdgeConfig<S, E> getEdgeConfig();

    protected static class MachineConfiguration<S, E> {
        private final StateMachineConfigurationBuilder<S, E> configurationBuilder;
        private final StateMachineStateBuilder<S, E> stateBuilder;
        private final StateMachineTransitionBuilder<S, E> transitionBuilder;
        private final StateMachineListener<S, E> listener;
        private final TaskExecutor executor;

        public MachineConfiguration(StateMachineConfigurationBuilder<S, E> configurationBuilder, StateMachineStateBuilder<S, E> stateBuilder,
                StateMachineTransitionBuilder<S, E> transitionBuilder, StateMachineListener<S, E> listener, TaskExecutor executor) {
            this.configurationBuilder = configurationBuilder;
            this.stateBuilder = stateBuilder;
            this.transitionBuilder = transitionBuilder;
            this.listener = listener;
            this.executor = executor;
        }
    }

    protected static class Transition<S, E> {
        private final S source;
        private final S target;
        private final E event;

        public Transition(S source, S target, E event) {
            this.source = source;
            this.target = target;
            this.event = event;
        }
    }

    protected static class FlowEdgeConfig<S, E> {
        private final S initState;
        private final S finalState;
        private final S lastState;
        private final E lastEvent;
        private final S defaultFailureState;
        private final E failureHandled;

        public FlowEdgeConfig(S initState, S finalState, S lastState, E lastEvent, S defaultFailureState, E failureHandled) {
            this.initState = initState;
            this.finalState = finalState;
            this.lastState = lastState;
            this.lastEvent = lastEvent;
            this.defaultFailureState = defaultFailureState;
            this.failureHandled = failureHandled;
        }
    }
}
