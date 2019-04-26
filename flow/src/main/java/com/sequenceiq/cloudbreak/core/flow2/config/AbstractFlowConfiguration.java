package com.sequenceiq.cloudbreak.core.flow2.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
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

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.DefaultFlowTriggerCondition;
import com.sequenceiq.cloudbreak.core.flow2.EventConverterAdapter;
import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.FlowAdapter;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowEventListener;
import com.sequenceiq.cloudbreak.core.flow2.FlowFinalizeAction;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggerCondition;
import com.sequenceiq.cloudbreak.core.flow2.MessageFactory;
import com.sequenceiq.cloudbreak.core.flow2.RestartAction;
import com.sequenceiq.cloudbreak.core.flow2.StateConverterAdapter;
import com.sequenceiq.cloudbreak.core.flow2.restart.DefaultRestartAction;

public abstract class AbstractFlowConfiguration<S extends FlowState, E extends FlowEvent> implements FlowConfiguration<E> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFlowConfiguration.class);

    private StateMachineFactory<S, E> stateMachineFactory;

    private final Class<S> stateType;

    private final Class<E> eventType;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    @Qualifier("DefaultRestartAction")
    private DefaultRestartAction defaultRestartAction;

    protected AbstractFlowConfiguration(Class<S> stateType, Class<E> eventType) {
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

    @Override
    public Flow createFlow(String flowId, Long stackId) {
        StateMachine<S, E> sm = stateMachineFactory.getStateMachine();
        FlowEventListener<S, E> fl = (FlowEventListener<S, E>) applicationContext.getBean(FlowEventListener.class, getEdgeConfig().initState,
                getEdgeConfig().finalState, getClass().getSimpleName(), flowId, stackId);
        Flow flow = new FlowAdapter<>(flowId, sm, new MessageFactory<>(), new StateConverterAdapter<>(stateType),
                new EventConverterAdapter<>(eventType), (Class<? extends FlowConfiguration<E>>) getClass(), fl);
        sm.addStateListener(fl);
        return flow;
    }

    @Override
    public FlowTriggerCondition getFlowTriggerCondition() {
        return applicationContext.getBean(DefaultFlowTriggerCondition.class);
    }

    protected StateMachineFactory<S, E> getStateMachineFactory() {
        return stateMachineFactory;
    }

    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    private void configure(StateMachineStateConfigurer<S, E> stateConfig, StateMachineTransitionConfigurer<S, E> transitionConfig,
            FlowEdgeConfig<S, E> flowEdgeConfig, Iterable<Transition<S, E>> transitions) throws Exception {
        StateConfigurer<S, E> stateConfigurer = stateConfig.withStates().initial(flowEdgeConfig.initState).end(flowEdgeConfig.finalState);
        ExternalTransitionConfigurer<S, E> transitionConfigurer = null;
        Collection<S> failHandled = new HashSet<>();
        for (Transition<S, E> transition : transitions) {
            transitionConfigurer = transitionConfigurer == null ? transitionConfig.withExternal() : transitionConfigurer.and().withExternal();
            AbstractAction<S, E, ?, ?> action = getAction(transition.source);
            if (action != null) {
                stateConfigurer.state(transition.source, action, null);
            }
            transitionConfigurer.source(transition.source).target(transition.target).event(transition.event);
            if (action != null && transition.getFailureEvent() != null && !Objects.equals(transition.target, flowEdgeConfig.defaultFailureState)) {
                action.setFailureEvent(transition.getFailureEvent());
                S failureState = Optional.ofNullable(transition.getFailureState()).orElse(flowEdgeConfig.defaultFailureState);
                stateConfigurer.state(failureState, getAction(failureState), null);
                transitionConfigurer.and().withExternal().source(transition.source).target(failureState).event(transition.getFailureEvent());
                if (!failHandled.contains(failureState)) {
                    failHandled.add(failureState);
                    transitionConfigurer.and().withExternal().source(failureState).target(flowEdgeConfig.finalState).event(flowEdgeConfig.failureHandled);
                }
            }
        }
        stateConfigurer.state(flowEdgeConfig.finalState, getAction(FlowFinalizeAction.class), null);
    }

    protected MachineConfiguration<S, E> getStateMachineConfiguration() {
        StateMachineConfigurationBuilder<S, E> configurationBuilder =
                new StateMachineConfigurationBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineStateBuilder<S, E> stateBuilder =
                new StateMachineStateBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineTransitionBuilder<S, E> transitionBuilder =
                new StateMachineTransitionBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineListener<S, E> listener =
                new StateMachineListenerAdapter<>() {
                    @Override
                    public void stateChanged(State<S, E> from, State<S, E> to) {
                        LOGGER.debug("state changed from {} to {}", from, to);
                    }

                    @Override
                    public void eventNotAccepted(Message<E> event) {
                        LOGGER.error("{} not accepted event: {}", getClass().getSimpleName(), event);
                    }
                };
        return new MachineConfiguration<>(configurationBuilder, stateBuilder, transitionBuilder, listener, new SyncTaskExecutor());
    }

    protected abstract List<Transition<S, E>> getTransitions();

    protected abstract FlowEdgeConfig<S, E> getEdgeConfig();

    private AbstractAction<S, E, ?, ?> getAction(FlowState state) {
        return state.action() == null ? getAction(state.name()) : getAction(state.action());
    }

    private AbstractAction<S, E, ?, ?> getAction(Class<? extends AbstractAction<?, ?, ?, ?>> clazz) {
        return (AbstractAction<S, E, ?, ?>) applicationContext.getBean(clazz.getSimpleName(), clazz);
    }

    private AbstractAction<S, E, ?, ?> getAction(String name) {
        try {
            return applicationContext.getBean(name, AbstractAction.class);
        } catch (NoSuchBeanDefinitionException ignored) {
            return null;
        }
    }

    @Override
    public RestartAction getRestartAction(String event) {
        Optional<Transition<S, E>> transaction = getTransitions().stream().filter(t -> t.event.event().equals(event)).findFirst();
        if (transaction.isPresent() && transaction.get().target.restartAction() != null) {
            Class<? extends RestartAction> restartAction = transaction.get().target.restartAction();
            return applicationContext.getBean(restartAction.getSimpleName(), restartAction);
        }
        return defaultRestartAction;
    }

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

    public static class Transition<S extends FlowState, E extends FlowEvent> {
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

            private final List<Transition<S, E>> transitions = new ArrayList<>();

            private Optional<E> defaultFailureEvent = Optional.empty();

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

            public void addTransition(S from, S to, E with) {
                transitions.add(new Transition<>(from, to, with, null, null));
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
                builder = b;
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
                builder = b;
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
                builder = b;
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

            public Builder<S, E> noFailureEvent() {
                builder.addTransition(from, to, with);
                return builder;
            }
        }
    }

    public static class FlowEdgeConfig<S, E> {
        private final S initState;

        private final S finalState;

        private final S defaultFailureState;

        private final E failureHandled;

        public FlowEdgeConfig(S initState, S finalState, S defaultFailureState, E failureHandled) {
            this.initState = initState;
            this.finalState = finalState;
            this.defaultFailureState = defaultFailureState;
            this.failureHandled = failureHandled;
        }
    }
}
