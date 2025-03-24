package com.sequenceiq.flow.graph;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.transition.Transition;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.FlowConfiguration;

public class FlowConfigDotGraphGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowConfigDotGraphGenerator.class);

    private static final String OUT_PATH = "build/diagrams/flow";

    private final ConfigurableApplicationContext applicationContext;

    private final FlowConfiguration<?> flowConfiguration;

    public FlowConfigDotGraphGenerator(ConfigurableApplicationContext appContext, FlowConfiguration<?> flowConfiguration) {
        this.applicationContext = appContext;
        this.flowConfiguration = flowConfiguration;
    }

    public void generate() throws Exception {
        LOGGER.info("Generating graph for flow: {}", this.flowConfiguration.getClass().getName());
        StringBuilder builder = new StringBuilder("digraph {\n");
        Field applicationContextField = Objects.requireNonNull(ReflectionUtils.findField(flowConfiguration.getClass(), "applicationContext"));
        ReflectionUtils.makeAccessible(applicationContextField);
        ReflectionUtils.setField(applicationContextField, flowConfiguration, applicationContext);
        Flow flow = initializeFlow();
        StateMachine<FlowState, FlowEvent> stateMachine = getStateMachine(flow);
        FlowState init = stateMachine.getInitialState().getId();
        builder.append(generateStartPoint(init, flowConfiguration.getClass().getSimpleName())).append('\n');
        List<Transition<FlowState, FlowEvent>> transitions = (List<Transition<FlowState, FlowEvent>>) stateMachine.getTransitions();
        Map<String, FlowState> transitionsAlreadyDefined = new HashMap<>();
        transitionsAlreadyDefined.put(init.toString(), init);
        while (!transitions.isEmpty()) {
            for (Transition<FlowState, FlowEvent> transition : new ArrayList<>(transitions)) {
                FlowState source = transition.getSource().getId();
                FlowState target = transition.getTarget().getId();
                if (transitionsAlreadyDefined.values().contains(source)) {
                    buildAndAddAGraphNodeWithTransitionDetails(transition, source, target, transitionsAlreadyDefined, builder, Boolean.FALSE);
                    transitions.remove(transition);
                } else {
                    LOGGER.info("A graph node which is not a target just a source, probably left to provide backward compatibility");
                    buildAndAddAGraphNodeWithTransitionDetails(transition, source, target, transitionsAlreadyDefined, builder, Boolean.TRUE);
                    transitions.remove(transition);
                }
            }
        }
        String dotGraphDescription = builder.append('}').toString();
        GraphDescriptorFileWriter.saveToFile(OUT_PATH, flowConfiguration.getClass().getSimpleName(), dotGraphDescription);
    }

    private Flow initializeFlow() throws Exception {
        ((AbstractFlowConfiguration<?, ?>) flowConfiguration).init();
        Flow flow = flowConfiguration.createFlow("", "", 0L, OperationType.UNKNOWN.name());
        flow.initialize(Map.of());
        return flow;
    }

    private StateMachine<FlowState, FlowEvent> getStateMachine(Flow flow) throws NoSuchFieldException, IllegalAccessException {
        Field flowMachine = flow.getClass().getDeclaredField("flowMachine");
        flowMachine.setAccessible(true);
        return (StateMachine<FlowState, FlowEvent>) flowMachine.get(flow);
    }

    private String generateStartPoint(FlowState name, String label) {
        return String.format("%s [label=\"%s\" shape=octagon color=green];", name, label);
    }

    private void buildAndAddAGraphNodeWithTransitionDetails(
            Transition<FlowState, FlowEvent> transition,
            FlowState source, FlowState target,
            Map<String, FlowState> transitionsAlreadyDefined,
            StringBuilder builder,
            boolean sourceEdge) {
        String id = generateTransitionId(source, target, transition.getTrigger().getEvent());
        if (!transitionsAlreadyDefined.keySet().contains(id)) {
            if (target.action() != null && !transitionsAlreadyDefined.values().contains(target)) {
                builder.append(generateState(target, target.action().getSimpleName())).append('\n');
            }
            builder.append(generateTransition(source, target, transition.getTrigger().getEvent(), sourceEdge)).append('\n');
            transitionsAlreadyDefined.put(id, target);
        }
    }

    private String generateTransitionId(FlowState source, FlowState target, FlowEvent event) {
        return source.toString() + target.toString() + event.toString();
    }

    private String generateState(FlowState state, String action) {
        return String.format("%s [label=\"%s\\n%s\" shape=rect color=black];", state, state, action);
    }

    private String generateTransition(FlowState source, FlowState target, FlowEvent event, boolean sourceEdge) {
        String color = "black";
        String style = "solid";
        if (Objects.equals(source, target)) {
            color = "blue";
        } else if (event.name().contains("FAIL") || event.name().contains("ERROR")) {
            color = "red";
            style = "dashed";
        } else if (sourceEdge) {
            color = "green";
            style = "dashed";
        }
        return String.format("%s -> %s [label=\"%s\" color=%s style=%s];", source, target, event, color, style);
    }
}
