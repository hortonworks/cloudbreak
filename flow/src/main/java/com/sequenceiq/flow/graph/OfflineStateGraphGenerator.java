package com.sequenceiq.flow.graph;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.util.ReflectionUtils;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowEventListener;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.core.metrics.FlowEventMetricListener;

public class OfflineStateGraphGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(OfflineStateGraphGenerator.class);

    private static final ConfigurableApplicationContext APP_CONTEXT = new CustomApplicationContext();

    static {
        APP_CONTEXT.refresh();
    }

    public OfflineStateGraphGenerator() {
    }

    public void collectFlowConfigsAndGenerateGraphs(String flowConfigsPackageName) throws IOException {
        LOGGER.info("Starting to collect flow configurations from '{}' package", flowConfigsPackageName);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        ImmutableSet<ClassPath.ClassInfo> classesOfFlowConfigPackage = ClassPath.from(classLoader).getTopLevelClassesRecursive(flowConfigsPackageName);
        Set<FlowConfiguration<? extends FlowEvent>> flowConfigurations = gatherFlowConfigurationsFromPackage(classesOfFlowConfigPackage);
        LOGGER.info("Collected number of Flow configuration: '{}', from package: '{}'", flowConfigurations.size(), flowConfigsPackageName);
        for (FlowConfiguration<?> flowConfiguration : flowConfigurations) {
            try {
                new FlowConfigDotGraphGenerator(APP_CONTEXT, flowConfiguration).generate();
            } catch (Exception ex) {
                LOGGER.error("Failed to generate flow graph for config: '{}'", flowConfiguration.getClass().getName());
            }
        }

        generateGraphsFromFlowChainConfigs(classesOfFlowConfigPackage, flowConfigurations);
    }

    private void generateGraphsFromFlowChainConfigs(ImmutableSet<ClassPath.ClassInfo> classesOfFlowConfigPackage,
            Set<FlowConfiguration<? extends FlowEvent>> flowConfigurations) {
        List<Class<FlowEventChainFactory<? extends Payload>>> flowEventChainFactories = classesOfFlowConfigPackage.stream()
                .filter(classInfo -> !classInfo.getName().contains("Test"))
                .map(ClassPath.ClassInfo::load)
                .filter(aClass -> !Modifier.isAbstract(aClass.getModifiers()) && !Modifier.isInterface(aClass.getModifiers()))
                .filter(FlowEventChainFactory.class::isAssignableFrom)
                .map(clazz -> (Class<FlowEventChainFactory<? extends Payload>>) clazz)
                .toList();
        LOGGER.info("Collected the number of flow chains: {}", flowEventChainFactories.size());

        for (Class<FlowEventChainFactory<? extends Payload>> flowChainFactoryClass : flowEventChainFactories) {
            try {
                Constructor<FlowEventChainFactory<? extends Payload>> constructor = ReflectionUtils.accessibleConstructor(flowChainFactoryClass);
                FlowEventChainFactory<? extends Payload> flowEventChainFactory = constructor.newInstance();
                FlowChainConfigDotGraphGenerator flowChainConfigDotGraphGenerator = new FlowChainConfigDotGraphGenerator(flowEventChainFactory);
                flowChainConfigDotGraphGenerator.generateGraphAndSaveToFile(flowConfigurations);
            } catch (Exception e) {
                LOGGER.warn("Failed to generate graph for flow chain: '{}'", flowChainFactoryClass.getName());
            }
        }
    }

    private Set<FlowConfiguration<? extends FlowEvent>> gatherFlowConfigurationsFromPackage(ImmutableSet<ClassPath.ClassInfo> classesOfFlowConfigPackage) {
        List<? extends Class<FlowConfiguration<? extends FlowEvent>>> classes = classesOfFlowConfigPackage.stream()
                .filter(classInfo -> !classInfo.getName().contains("Test"))
                .map(ClassPath.ClassInfo::load)
                .filter(aClass -> !Modifier.isAbstract(aClass.getModifiers()) && !Modifier.isInterface(aClass.getModifiers()))
                .filter(FlowConfiguration.class::isAssignableFrom)
                .map(clazz -> (Class<FlowConfiguration<? extends FlowEvent>>) clazz)
                .toList();

        Set<FlowConfiguration<? extends FlowEvent>> flowConfigurations = new HashSet<>();
        for (Class<FlowConfiguration<? extends FlowEvent>> flowConfigurationClass : classes) {
            try {
                LOGGER.info("Trying to add flow config with class name: '{}'", flowConfigurationClass.getName());
                Constructor<FlowConfiguration<? extends FlowEvent>> constructor = ReflectionUtils.accessibleConstructor(flowConfigurationClass);
                flowConfigurations.add(constructor.newInstance());
            } catch (Exception exception) {
                LOGGER.warn("Failed to instantiate flow config with class name: '{}'", flowConfigurationClass.getName(), exception);
            }
        }
        return flowConfigurations;
    }

    static class CustomPayload implements Payload {

        @Override
        public Long getResourceId() {
            return 0L;
        }

        @Override
        public Exception getException() {
            return Payload.super.getException();
        }
    }

    static class CustomApplicationContext extends AbstractApplicationContext {

        @Override
        public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
            return (T) new CustomAction();
        }

        @Override
        protected void refreshBeanFactory() throws BeansException {
        }

        @Override
        protected void closeBeanFactory() {

        }

        @Override
        protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
            return new CustomBeanFactory();
        }

        @Override
        public ConfigurableListableBeanFactory getBeanFactory() {
            return obtainFreshBeanFactory();
        }
    }

    static class CustomBeanFactory extends DefaultListableBeanFactory {
        @Override
        public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
            if (requiredType.equals(FlowEventListener.class)) {

                return (T) new CustomCDPFlowStructuredEventHandler();
            } else if (requiredType.equals(FlowEventMetricListener.class)) {
                return (T) new CustomFlowEventMetricListener();
            } else {
                return null;
            }
        }
    }

    static class CustomCDPFlowStructuredEventHandler implements FlowEventListener {

        CustomCDPFlowStructuredEventHandler() {
        }

        @Override
        public void stateChanged(State state, State state1) {
        }

        @Override
        public void stateEntered(State state) {
        }

        @Override
        public void stateExited(State state) {
        }

        @Override
        public void eventNotAccepted(Message message) {
        }

        @Override
        public void transition(Transition transition) {
        }

        @Override
        public void transitionStarted(Transition transition) {
        }

        @Override
        public void transitionEnded(Transition transition) {
        }

        @Override
        public void stateMachineStarted(StateMachine stateMachine) {
        }

        @Override
        public void stateMachineStopped(StateMachine stateMachine) {
        }

        @Override
        public void stateMachineError(StateMachine stateMachine, Exception e) {
        }

        @Override
        public void extendedStateChanged(Object o, Object o1) {
        }

        @Override
        public void stateContext(StateContext stateContext) {
        }

        @Override
        public void setException(Exception exception) {
        }
    }

    static class CustomAction extends AbstractAction<FlowState, FlowEvent, CommonContext, Payload> {
        CustomAction() {
            super(Payload.class);
        }

        @Override
        public void execute(StateContext<FlowState, FlowEvent> context) {
        }

        @Override
        protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, Payload payload) {
            return null;
        }

        @Override
        protected void doExecute(CommonContext context, Payload payload, Map<Object, Object> variables) {

        }

        @Override
        protected Selectable createRequest(CommonContext context) {
            return null;
        }

        @Override
        protected Object getFailurePayload(Payload payload, Optional<CommonContext> flowContext, Exception ex) {
            return null;
        }
    }

    static class CustomFlowEventMetricListener extends FlowEventMetricListener {

        CustomFlowEventMetricListener() {
            super(null, null, null, null, 0);
        }

        @Override
        public void transitionEnded(Transition transition) {
        }
    }
}
