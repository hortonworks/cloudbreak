package com.sequenceiq.cloudbreak.core.flow2.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.transition.Transition;

import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFlowConfig;

public class OfflineStateGenerator {

    private static final List<FlowConfiguration<? extends FlowState, ? extends FlowEvent>> CONFIGS =
            Arrays.<FlowConfiguration<? extends FlowState, ? extends FlowEvent>>asList(
                new ClusterTerminationFlowConfig(),
                new InstanceTerminationFlowConfig(),
                new StackCreationFlowConfig(),
                new StackStartFlowConfig(),
                new StackStopFlowConfig(),
                new StackSyncFlowConfig(),
                new StackTerminationFlowConfig()
        );

    private static final ApplicationContext APP_CONTEXT = new CustomApplicationContext();

    private OfflineStateGenerator() {

    }

    public static void main(String[] args) throws Exception {
        new OfflineStateGenerator().generate();
    }

    private void generate() throws Exception {
        for (FlowConfiguration flowConfiguration : CONFIGS) {
            Field applicationContext = flowConfiguration.getClass().getSuperclass().getDeclaredField("applicationContext");
            applicationContext.setAccessible(true);
            applicationContext.set(flowConfiguration, APP_CONTEXT);
            ((AbstractFlowConfiguration) flowConfiguration).init();
            Flow flow = flowConfiguration.createFlow("");
            flow.initialize();
            Field flowMachine = flow.getClass().getDeclaredField("flowMachine");
            flowMachine.setAccessible(true);
            StateMachine<FlowState, FlowEvent> stateMachine = (StateMachine) flowMachine.get(flow);
            FlowState init = stateMachine.getInitialState().getId();
            echo(":::::: " + flowConfiguration.getClass().getSimpleName());
            List<Transition<FlowState, FlowEvent>> transitions = (List<Transition<FlowState, FlowEvent>>) stateMachine.getTransitions();
            Map<FlowState, Integer> stateLevels = new HashMap<>();
            stateLevels.put(init, 1);
            while (!transitions.isEmpty()) {
                for (Transition<FlowState, FlowEvent> transition : new ArrayList<>(transitions)) {
                        FlowState source = transition.getSource().getId();
                        FlowState target = transition.getTarget().getId();
                        Integer parentLevel = stateLevels.get(source);
                        if (parentLevel != null) {
                            transitions.remove(transition);
                            Integer level = parentLevel + 1;
                            stateLevels.put(target, level);
                            echo(getLevelRepresentation(level) + source + " -(" + transition.getTrigger().getEvent() + ")-> " + target
                                    + (target.action() != null ? " : " + target.action().getSimpleName() : ""));
                        }
                }
            }
        }
    }

    private void echo(String output) {
        System.out.println(":: " + output);
    }

    private String getLevelRepresentation(Integer level) {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < level; i++) {
            builder.append("-");
        }
        return builder.append(" ").toString();
    }

    static class CustomApplicationContext extends AbstractApplicationContext {

        @Override
        public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
            return null;
        }

        @Override
        protected void refreshBeanFactory() throws BeansException {

        }

        @Override
        protected void closeBeanFactory() {

        }

        @Override
        public ConfigurableListableBeanFactory getBeanFactory() {
            return null;
        }
    }
}
