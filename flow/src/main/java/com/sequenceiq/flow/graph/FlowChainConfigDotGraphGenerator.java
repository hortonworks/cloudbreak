package com.sequenceiq.flow.graph;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.config.FlowConfiguration;

public class FlowChainConfigDotGraphGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowChainConfigDotGraphGenerator.class);

    private static final String OUT_PATH = "build/diagrams/flow/chain";

    private final FlowEventChainFactory<? extends Payload> flowEventChainFactory;

    public FlowChainConfigDotGraphGenerator(FlowEventChainFactory<? extends Payload> flowEventChainFactory) {
        this.flowEventChainFactory = flowEventChainFactory;
    }

    public String generateGraphAndSaveToFile(Set<FlowConfiguration<? extends FlowEvent>> flowConfigurations)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        String graphDescriptor = generateGraph(flowConfigurations);
        GraphDescriptorFileWriter.saveToFile(OUT_PATH, flowEventChainFactory.getClass().getSimpleName(), graphDescriptor);
        return graphDescriptor;
    }

    public String generateGraph(Set<FlowConfiguration<? extends FlowEvent>> flowConfigurations)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        Class<?> flowChainFactoryClass = flowEventChainFactory.getClass();
        FlowTriggerEventQueue flowTriggerEventQueue = executeCreateFlowTriggerEventQueueWithType(flowEventChainFactory);
        LOGGER.info("Generating graph descriptor for flow chain: '{}'  with FlowChainTriggerEvent:{}", flowChainFactoryClass.getName(),
                flowTriggerEventQueue.getTriggerEvent());
        StringBuilder graphDescriptorBuilder = new StringBuilder("digraph {\n");
        String currentState = "INIT_STATE";
        graphDescriptorBuilder.append(String.format("%s [label=\"%s\" shape=tripleoctagon color=blue];", currentState, flowChainFactoryClass.getSimpleName()))
                .append('\n');

        for (Selectable selectable : flowTriggerEventQueue.getQueue()) {
            String nextItemTriggerEvent = selectable.getSelector();
            Optional<FlowConfiguration<? extends FlowEvent>> flowConfiguration = flowConfigurations.stream()
                    .filter(flowConfig -> Arrays.stream(flowConfig.getInitEvents()).anyMatch(flowEvent -> nextItemTriggerEvent.equals(flowEvent.event())))
                    .findFirst();

            if (flowConfiguration.isPresent()) {
                String flowConfigName = flowConfiguration.get().getClass().getSimpleName();
                graphDescriptorBuilder.append(String.format("%s [shape=octagon color=green]", flowConfigName))
                        .append('\n')
                        .append(String.format("%s -> %s [label=\"%s\" style=solid];", currentState, flowConfigName, nextItemTriggerEvent))
                        .append('\n');
                currentState = flowConfigName;
            } else {
                graphDescriptorBuilder.append(String.format("%s [shape=tripleoctagon color=green]", nextItemTriggerEvent))
                        .append('\n')
                        .append(String.format("%s -> %s [label=\"%s\" style=solid];", currentState, nextItemTriggerEvent, nextItemTriggerEvent))
                        .append('\n');
                currentState = nextItemTriggerEvent;
            }
        }
        graphDescriptorBuilder.append("}");
        return graphDescriptorBuilder.toString();
    }

    private <T extends Payload> FlowTriggerEventQueue executeCreateFlowTriggerEventQueueWithType(FlowEventChainFactory<T> flowEventChainFactory)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        for (Type type : flowEventChainFactory.getClass().getGenericInterfaces()) {
            if (type instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) type;
                if (pType.getRawType().equals(FlowEventChainFactory.class)) {
                    Type actualTypeArgument = pType.getActualTypeArguments()[0];
                    Class<? extends Payload> concreteType = (Class<? extends Payload>) actualTypeArgument;
                    T payload = (T) ReflectionUtils.accessibleConstructor(concreteType, Long.class).newInstance(0L);
                    return flowEventChainFactory.createFlowTriggerEventQueue(payload);
                }
            }
        }
        throw new IllegalArgumentException("Not able to find suitable default constructor and execute the gathering of flow chain information from config");
    }

}
