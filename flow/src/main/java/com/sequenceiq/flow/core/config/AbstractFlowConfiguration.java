package com.sequenceiq.flow.core.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

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
import org.springframework.statemachine.config.model.ConfigurationData;
import org.springframework.statemachine.config.model.DefaultStateMachineModel;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.DefaultFlowTriggerCondition;
import com.sequenceiq.flow.core.EventConverterAdapter;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowAdapter;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowEventListener;
import com.sequenceiq.flow.core.FlowFinalizeAction;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.MessageFactory;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.StateConverterAdapter;
import com.sequenceiq.flow.core.listener.FlowEventCommonListener;
import com.sequenceiq.flow.core.listener.FlowTransitionContext;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

public abstract class AbstractFlowConfiguration<S extends FlowState, E extends FlowEvent> implements FlowConfiguration<E> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFlowConfiguration.class);

    private final Class<S> stateType;

    private final Class<E> eventType;

    private StateMachineFactory<S, E> stateMachineFactory;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    @Qualifier("DefaultRestartAction")
    private DefaultRestartAction defaultRestartAction;

    @Inject
    private FlowLogDBService flowLogDBService;

    protected AbstractFlowConfiguration(Class<S> stateType, Class<E> eventType) {
        this.stateType = stateType;
        this.eventType = eventType;
    }

    @PostConstruct
    public void init() throws Exception {
        MachineConfiguration<S, E> config = getStateMachineConfiguration();
        config.configurationBuilder.withConfiguration().listener(config.listener).taskExecutor(config.executor);
        ConfigurationData<S, E> configurationData = config.configurationBuilder.build();
        config.transitionBuilder.setSharedObject(ConfigurationData.class, configurationData);
        config.stateBuilder.setSharedObject(ConfigurationData.class, configurationData);
        configure(config.stateBuilder, config.transitionBuilder, getEdgeConfig(), getTransitions());
        stateMachineFactory = new ObjectStateMachineFactory<>(
                new DefaultStateMachineModel<>(configurationData, config.stateBuilder.build(), config.transitionBuilder.build()));
    }

    @Override
    public Flow createFlow(String flowId, String flowChainId, Long resourceId, String flowChainType) {
        StateMachine<S, E> sm = stateMachineFactory.getStateMachine();
        FlowEventListener<S, E> fl = (FlowEventListener<S, E>) applicationContext.getBean(FlowEventListener.class, getEdgeConfig().initState,
                getEdgeConfig().finalState, flowChainType, getClass().getSimpleName(), flowChainId, flowId, resourceId);
        Flow flow = new FlowAdapter<>(flowId, sm, new MessageFactory<>(), new StateConverterAdapter<>(stateType),
                new EventConverterAdapter<>(eventType), (Class<? extends FlowConfiguration<E>>) getClass(), fl);
        sm.addStateListener(fl);

        FlowTransitionContext flowTransitionContext = new FlowTransitionContext(getEdgeConfig(), flowChainType, getClass().getSimpleName(), getStateType(),
                resourceId, flowId, flowChainId, System.currentTimeMillis());
        FlowEventCommonListener<S, E> flowEventMetricsListener = applicationContext.getBean(FlowEventCommonListener.class, flowTransitionContext);
        sm.addStateListener(flowEventMetricsListener);
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
                action.setFlowEdgeConfig(flowEdgeConfig);
            }
            transitionConfigurer.source(transition.source).target(transition.target).event(transition.event);
            if (action != null && transition.getFailureEvent() != null && !Objects.equals(transition.target, flowEdgeConfig.defaultFailureState)) {
                action.setFailureEvent(transition.getFailureEvent());
                S failureState = Optional.ofNullable(transition.getFailureState()).orElse(flowEdgeConfig.defaultFailureState);
                AbstractAction<S, E, ?, ?> failureAction = getAction(failureState);
                if (failureAction != null) {
                    failureAction.setFlowEdgeConfig(flowEdgeConfig);
                    failureAction.setFailureStateId(failureState.name());
                }
                stateConfigurer.state(failureState, failureAction, null);
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
                        FlowParameters flowParameters = (FlowParameters) event.getHeaders().get(MessageFactory.HEADERS.FLOW_PARAMETERS.name());
                        flowLogDBService.closeFlowOnError(flowParameters.getFlowId(), String.format("%s not accepted event: %s",
                                getClass().getSimpleName(), event.getPayload() != null ? event.getPayload().getClass().getName() : "missing payload"));
                    }
                };
        return new MachineConfiguration<>(configurationBuilder, stateBuilder, transitionBuilder, listener, new SyncTaskExecutor());
    }

    protected abstract List<Transition<S, E>> getTransitions();

    public abstract FlowEdgeConfig<S, E> getEdgeConfig();

    public E getFailHandledEvent() {
        return getEdgeConfig().failureHandled;
    }

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
        if (event == null) {
            Class<? extends RestartAction> restartAction = getTransitions().get(getTransitions().size() - 1).target.restartAction();
            return applicationContext.getBean(restartAction.getSimpleName(), restartAction);
        } else {
            Optional<Transition<S, E>> transition = getTransitions().stream().filter(t -> t.event.event().equals(event)).findFirst();
            if (transition.isPresent() && transition.get().target.restartAction() != null) {
                Class<? extends RestartAction> restartAction = transition.get().target.restartAction();
                return applicationContext.getBean(restartAction.getSimpleName(), restartAction);
            }
            return defaultRestartAction;
        }
    }

    public Class<S> getStateType() {
        return stateType;
    }

    public Class<E> getEventType() {
        return eventType;
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

        public S getSource() {
            return source;
        }

        public S getTarget() {
            return target;
        }

        public E getEvent() {
            return event;
        }

        public S getFailureState() {
            return failureState;
        }

        public E getFailureEvent() {
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

    public static class FlowEdgeConfig<S extends FlowState, E extends FlowEvent> {
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

        public S getInitState() {
            return initState;
        }

        public S getFinalState() {
            return finalState;
        }

        public S getDefaultFailureState() {
            return defaultFailureState;
        }

        public E getFailureHandled() {
            return failureHandled;
        }
    }
}
