package com.sequenceiq.flow.graph;

import static com.sequenceiq.flow.core.chain.init.config.FlowChainInitEvent.FLOWCHAIN_INIT_TRIGGER_EVENT;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeEvent;
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
        FlowTriggerEventQueue flowTriggerEventQueue = executeCreateFlowTriggerEventQueueWithType(flowEventChainFactory);
        return generateGraphAndSaveToFile(flowConfigurations, flowTriggerEventQueue, "");
    }

    public String generateGraphAndSaveToFile(Set<FlowConfiguration<? extends FlowEvent>> flowConfigurations, FlowTriggerEventQueue flowTriggerEventQueue,
            String fileNameSuffix) throws IOException {
        String graphDescriptor = generateGraph(flowConfigurations, flowTriggerEventQueue);
        String outFileName = flowEventChainFactory.getName();
        if (StringUtils.isNotEmpty(fileNameSuffix)) {
            outFileName = outFileName + "-" + fileNameSuffix;
        }
        GraphDescriptorFileWriter.saveToFile(OUT_PATH, outFileName, graphDescriptor);
        return graphDescriptor;
    }

    private String generateGraph(Set<FlowConfiguration<? extends FlowEvent>> flowConfigurations, FlowTriggerEventQueue flowTriggerEventQueue) {
        Class<?> flowChainFactoryClass = flowEventChainFactory.getClass();
        LOGGER.info("Generating graph descriptor for flow chain: '{}'  with FlowChainTriggerEvent:{}", flowChainFactoryClass.getName(),
                flowTriggerEventQueue.getTriggerEvent());
        StringBuilder graphDescriptorBuilder = new StringBuilder("digraph {\n");
        String currentState = "INIT_STATE";
        graphDescriptorBuilder.append(generateLegendDescription())
                .append(String.format("%s [label=\"%s\" shape=tripleoctagon color=blue];", currentState, flowChainFactoryClass.getSimpleName()))
                .append('\n');

        for (Selectable selectable : flowTriggerEventQueue.getQueue()) {
            String nextItemTriggerEvent = selectable.getSelector();
            if (!FLOWCHAIN_INIT_TRIGGER_EVENT.event().equals(nextItemTriggerEvent)) {
                Optional<FlowConfiguration<? extends FlowEvent>> flowConfiguration = flowConfigurations.stream()
                        .filter(flowConfig -> Arrays.stream(flowConfig.getInitEvents()).anyMatch(flowEvent -> nextItemTriggerEvent.equals(flowEvent.event())))
                        .findFirst();

                if (flowConfiguration.isPresent()) {
                    String flowConfigName = flowConfiguration.get().getClass().getSimpleName();
                    graphDescriptorBuilder.append(String.format("%s [shape=octagon color=green];", flowConfigName))
                            .append('\n')
                            .append(String.format("%s -> %s [label=\"%s\" style=solid];", currentState, flowConfigName, nextItemTriggerEvent))
                            .append('\n');
                    currentState = flowConfigName;
                } else if (FlowChainFinalizeEvent.FLOWCHAIN_FINALIZE_TRIGGER_EVENT.event().equals(nextItemTriggerEvent)) {
                    String finalizedState = "FLOWCHAIN_FINALIZED";
                    graphDescriptorBuilder.append(String.format("%s [color=black]", finalizedState))
                            .append('\n')
                            .append(String.format("%s -> %s [label=\"%s\" style=solid];", currentState, finalizedState, nextItemTriggerEvent))
                            .append('\n');
                } else {
                    graphDescriptorBuilder.append(String.format("%s [shape=tripleoctagon color=green tooltip=\"Flow Chain\"]", nextItemTriggerEvent))
                            .append('\n')
                            .append(String.format("%s -> %s [label=\"%s\" style=solid];", currentState, nextItemTriggerEvent, nextItemTriggerEvent))
                            .append('\n');
                    currentState = nextItemTriggerEvent;
                }
            }
        }
        graphDescriptorBuilder.append("}");
        return graphDescriptorBuilder.toString();
    }

    private String generateLegendDescription() {
        return """
                subgraph cluster_key {
                    label="Graph Node types";
                    labelloc=b;
                    FLOW_CHAIN [label="Flow Chain" shape=tripleoctagon color=green];
                    FLOW [label="Flow" shape=octagon color=green];
                    TERMINAL_STATE [label="Terminal State"];
                    FLOW_CHAIN -> FLOW  [style=invis];
                    FLOW -> TERMINAL_STATE [style=invis];
                }
                """;
    }

    private <T extends Payload> FlowTriggerEventQueue executeCreateFlowTriggerEventQueueWithType(FlowEventChainFactory<T> flowEventChainFactory)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        for (Type type : flowEventChainFactory.getClass().getGenericInterfaces()) {
            if (type instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) type;
                if (pType.getRawType().equals(FlowEventChainFactory.class)) {
                    Type actualTypeArgument = pType.getActualTypeArguments()[0];
                    Class<? extends Payload> concreteType = (Class<? extends Payload>) actualTypeArgument;
                    T payload;
                    try {
                        payload = (T) ReflectionUtils.accessibleConstructor(concreteType, Long.class).newInstance(0L);
                    } catch (NoSuchMethodException nm) {
                        payload = (T) ReflectionUtils.accessibleConstructor(concreteType).newInstance();
                    }
                    return flowEventChainFactory.createFlowTriggerEventQueue(payload);
                }
            }
        }
        throw new IllegalArgumentException("Not able to find suitable default constructor and execute the gathering of flow chain information from config");
    }

}
