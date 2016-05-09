package com.sequenceiq.cloudbreak.core.flow2.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.Message;
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
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.EventConverterAdapter;
import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.FlowAdapter;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowFinalizeAction;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.core.flow2.MessageFactory;
import com.sequenceiq.cloudbreak.core.flow2.StateConverterAdapter;

public abstract class AbstractFlowConfiguration<S extends FlowState, E extends FlowEvent> implements FlowConfiguration<E> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFlowConfiguration.class);

    private StateMachineFactory<S, E> stateMachineFactory;
    private final Class<S> stateType;
    private final Class<E> eventType;

    @Inject
    private ApplicationContext applicationContext;

    public AbstractFlowConfiguration(Class<S> stateType, Class<E> eventType) {
        this.stateType = stateType;
        this.eventType = eventType;
    }

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
                .initial(flowEdgeConfig.initState, flowEdgeConfig.initState.action() != null ? getAction(flowEdgeConfig.initState) : null)
                .end(flowEdgeConfig.finalState);
        ExternalTransitionConfigurer<S, E> transitionConfigurer = null;
        Set<S> failHandled = new HashSet<>();
        for (Transition<S, E> transition : transitions) {
            transitionConfigurer = transitionConfigurer == null ? transitionConfig.withExternal() : transitionConfigurer.and().withExternal();
            AbstractAction<S, E, ?, ?> action = getAction(transition.target);
            stateConfigurer.state(transition.target, action, null);
            transitionConfigurer.source(transition.source).target(transition.target).event(transition.event);
            if (transition.getFailureEvent() != null && transition.target != flowEdgeConfig.defaultFailureState) {
                action.setFailureEvent(transition.getFailureEvent());
                S failureState = Optional.fromNullable(transition.getFailureState()).or(flowEdgeConfig.defaultFailureState);
                stateConfigurer.state(failureState, getAction(failureState), null);
                transitionConfigurer.and().withExternal().source(transition.target).target(failureState).event(transition.getFailureEvent());
                if (!failHandled.contains(failureState)) {
                    failHandled.add(failureState);
                    transitionConfigurer.and().withExternal().source(failureState).target(flowEdgeConfig.finalState).event(flowEdgeConfig.failureHandled);
                }
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
                        LOGGER.error("{} not accepted event: {}", getClass().getSimpleName(), event);
                    }
                };
        return new MachineConfiguration<>(configurationBuilder, stateBuilder, transitionBuilder, listener, new SyncTaskExecutor());
    }

    @Override
    public Flow createFlow(String flowId) {
        return new FlowAdapter<>(flowId, getStateMachineFactory().getStateMachine(), new MessageFactory<E>(), new StateConverterAdapter<>(stateType),
                new EventConverterAdapter<>(eventType));
    }

    private AbstractAction getAction(FlowState state) {
        return state.action() == null ? getAction(state.name()) : getAction(state.action());
    }

    private AbstractAction getAction(Class<? extends AbstractAction> clazz) {
        return applicationContext.getBean(clazz.getSimpleName(), clazz);
    }

    private AbstractAction getAction(String name) {
        return applicationContext.getBean(name, AbstractAction.class);
    }

    protected abstract List<Transition<S, E>> getTransitions();

    protected abstract FlowEdgeConfig<S, E> getEdgeConfig();

    static class MachineConfiguration<S, E> {
        private final StateMachineConfigurationBuilder<S, E> configurationBuilder;
        private final StateMachineStateBuilder<S, E> stateBuilder;
        private final StateMachineTransitionBuilder<S, E> transitionBuilder;
        private final StateMachineListener<S, E> listener;
        private final TaskExecutor executor;

        MachineConfiguration(StateMachineConfigurationBuilder<S, E> configurationBuilder, StateMachineStateBuilder<S, E> stateBuilder,
                StateMachineTransitionBuilder<S, E> transitionBuilder, StateMachineListener<S, E> listener, TaskExecutor executor) {
            this.configurationBuilder = configurationBuilder;
            this.stateBuilder = stateBuilder;
            this.transitionBuilder = transitionBuilder;
            this.listener = listener;
            this.executor = executor;
        }
    }

    protected static class Transition<S extends FlowState, E extends FlowEvent> {
        private final S source;
        private final S target;
        private final E event;
        private final S failureState;
        private final E failureEvent;

        private Transition(S source, S target, E event, S failureState, E failureEvent) {
            this.source = source;
            this.target = target;
            this.event = event;
            this.failureState = failureState;
            this.failureEvent = failureEvent;
        }

        private S getFailureState() {
            return failureState;
        }

        private E getFailureEvent() {
            return failureEvent;
        }

        public static class Builder<S extends FlowState, E extends FlowEvent> {
            private List<Transition<S, E>> transitions = new ArrayList<>();
            private Optional<E> defaultFailureEvent = Optional.absent();

            public ToBuilder<S, E> from(S from) {
                return new ToBuilder<>(from, this);
            }

            public void addTransition(S from, S to, E with, S fail) {
                if (!defaultFailureEvent.isPresent()) {
                    throw new UnsupportedOperationException("Default failureEvent event must specified!");
                }
                addTransition(from, to, with, fail, defaultFailureEvent.get());
            }

            public void addTransition(S from, S to, E with, S fail, E withFailure) {
                transitions.add(new Transition<>(from, to, with, fail, withFailure));
            }

            public List<Transition<S, E>> build() {
                return transitions;
            }

            public Builder<S, E> defaultFailureEvent(E defaultFailureEvent) {
                this.defaultFailureEvent = Optional.of(defaultFailureEvent);
                return this;
            }
        }

        public static class ToBuilder<S extends FlowState, E extends FlowEvent> {
            private final S from;
            private final Builder<S, E> builder;

            ToBuilder(S from, Builder<S, E> b) {
                this.from = from;
                this.builder = b;
            }

            public WithBuilder<S, E> to(S to) {
                return new WithBuilder<>(from, to, builder);
            }
        }

        public static class WithBuilder<S extends FlowState, E extends FlowEvent> {
            private final S from;
            private final S to;
            private final Builder<S, E> builder;

            WithBuilder(S from, S to, Builder<S, E> b) {
                this.from = from;
                this.to = to;
                this.builder = b;
            }

            public FailureBuilder<S, E> event(E with) {
                return new FailureBuilder<>(from, to, with, builder);
            }
        }

        public static class FailureBuilder<S extends FlowState, E extends FlowEvent> {
            private final S from;
            private final S to;
            private final E with;
            private final Builder<S, E> builder;
            private S failure;

            FailureBuilder(S from, S to, E with, Builder<S, E> b) {
                this.from = from;
                this.to = to;
                this.with = with;
                this.builder = b;
            }

            public FailureBuilder<S, E> failureState(S toFailure) {
                failure = toFailure;
                return this;
            }

            public Builder<S, E> failureEvent(E withFailure) {
                builder.addTransition(from, to, with, failure, withFailure);
                return builder;
            }

            public Builder<S, E> defaultFailureEvent() {
                builder.addTransition(from, to, with, failure);
                return builder;
            }
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
