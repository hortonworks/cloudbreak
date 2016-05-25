package com.sequenceiq.cloudbreak.core.flow2.config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.transition.Transition;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleConfig;

public class OfflineStateGenerator {

    private static final String OUT_PATH = "build/diagrams/flow";

    private static final List<FlowConfiguration<? extends FlowEvent>> CONFIGS =
            Arrays.<FlowConfiguration<? extends FlowEvent>>asList(
                    new ClusterTerminationFlowConfig(),
                    new InstanceTerminationFlowConfig(),
                    new StackCreationFlowConfig(),
                    new StackStartFlowConfig(),
                    new StackStopFlowConfig(),
                    new StackSyncFlowConfig(),
                    new StackUpscaleConfig(),
                    new ClusterDownscaleFlowConfig(),
                    new StackDownscaleConfig(),
                    new StackTerminationFlowConfig(),
                    new ClusterUpscaleFlowConfig(),
                    new ClusterStartFlowConfig(),
                    new ClusterStopFlowConfig()
            );

    private static final ApplicationContext APP_CONTEXT = new CustomApplicationContext();

    private final FlowConfiguration flowConfiguration;

    private OfflineStateGenerator(FlowConfiguration flowConfiguration) {
        this.flowConfiguration = flowConfiguration;
    }

    public static void main(String[] args) throws Exception {
        for (FlowConfiguration flowConfiguration : CONFIGS) {
            new OfflineStateGenerator(flowConfiguration).generate();
        }
    }

    private void generate() throws Exception {
        StringBuilder builder = new StringBuilder("digraph {\n");
        injectAppContext(flowConfiguration);
        Flow flow = initializeFlow();
        StateMachine<FlowState, FlowEvent> stateMachine = getStateMachine(flow);
        FlowState init = stateMachine.getInitialState().getId();
        builder.append(generateStartPoint(init, flowConfiguration.getClass().getSimpleName())).append("\n");
        List<Transition<FlowState, FlowEvent>> transitions = (List<Transition<FlowState, FlowEvent>>) stateMachine.getTransitions();
        Map<String, FlowState> transitionsAlreadyDefined = new HashMap<>();
        transitionsAlreadyDefined.put(init.toString(), init);
        while (!transitions.isEmpty()) {
            for (Transition<FlowState, FlowEvent> transition : new ArrayList<>(transitions)) {
                FlowState source = transition.getSource().getId();
                FlowState target = transition.getTarget().getId();
                if (transitionsAlreadyDefined.values().contains(source)) {
                    String id = generateTransitionId(source, target, transition.getTrigger().getEvent());
                    if (!transitionsAlreadyDefined.keySet().contains(id)) {
                        if (target.action() != null && !transitionsAlreadyDefined.values().contains(target)) {
                            builder.append(generateState(target, target.action().getSimpleName())).append("\n");
                        }
                        builder.append(generateTransition(source, target, transition.getTrigger().getEvent())).append("\n");
                        transitionsAlreadyDefined.put(id, target);
                    }
                    transitions.remove(transition);
                }
            }
        }
        saveToFile(builder.append("}").toString());
    }

    private String generateTransitionId(FlowState source, FlowState target, FlowEvent event) {
        return source.toString() + target.toString() + event.toString();
    }

    private String generateStartPoint(FlowState name, String label) {
        return String.format("%s [label=\"%s\" shape=ellipse color=green];", name, label);
    }

    private String generateState(FlowState state, String action) {
        return String.format("%s [label=\"%s\\n%s\" shape=rect color=black];", state, state, action);
    }

    private String generateTransition(FlowState source, FlowState target, FlowEvent event) {
        String color = "black";
        String style = "solid";
        if (source == target) {
            color = "blue";
        } else if (event.name().indexOf("FAIL") != -1 || event.name().indexOf("ERROR") != -1) {
            color = "red";
            style = "dashed";
        }
        return String.format("%s -> %s [label=\"%s\" color=%s style=%s];", source, target, event, color, style);
    }

    private StateMachine<FlowState, FlowEvent> getStateMachine(Flow flow) throws NoSuchFieldException, IllegalAccessException {
        Field flowMachine = flow.getClass().getDeclaredField("flowMachine");
        flowMachine.setAccessible(true);
        return (StateMachine) flowMachine.get(flow);
    }

    private Flow initializeFlow() throws Exception {
        ((AbstractFlowConfiguration) flowConfiguration).init();
        Flow flow = flowConfiguration.createFlow("");
        flow.initialize();
        return flow;
    }

    private void saveToFile(String content) throws IOException {
        File destinationDir = new File(OUT_PATH);
        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        }
        Files.write(Paths.get(String.format("%s/%s.dot", OUT_PATH, flowConfiguration.getClass().getSimpleName())), content.getBytes());
    }

    private static void injectAppContext(FlowConfiguration flowConfiguration) throws IllegalAccessException, NoSuchFieldException {
        Field applicationContext = flowConfiguration.getClass().getSuperclass().getDeclaredField("applicationContext");
        applicationContext.setAccessible(true);
        applicationContext.set(flowConfiguration, APP_CONTEXT);
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
        public ConfigurableListableBeanFactory getBeanFactory() {
            return null;
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
        protected CommonContext createFlowContext(String flowId, StateContext<FlowState, FlowEvent> stateContext, Payload payload) {
            return null;
        }

        @Override
        protected void doExecute(CommonContext context, Payload payload, Map<Object, Object> variables) throws Exception {

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
}
