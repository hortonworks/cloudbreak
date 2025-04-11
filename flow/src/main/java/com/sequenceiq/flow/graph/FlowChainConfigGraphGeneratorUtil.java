package com.sequenceiq.flow.graph;

import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.config.FlowConfiguration;

public class FlowChainConfigGraphGeneratorUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowChainConfigGraphGeneratorUtil.class);

    private FlowChainConfigGraphGeneratorUtil() {
    }

    public static void generateFor(FlowEventChainFactory<? extends Payload> flowChainFactory, String flowConfigsPackage, FlowTriggerEventQueue eventQueues) {
        generateFor(flowChainFactory, flowConfigsPackage, eventQueues, "");
    }

    public static void generateFor(FlowEventChainFactory<? extends Payload> flowChainFactory, String flowConfigsPackage, FlowTriggerEventQueue eventQueues,
            String fileNameSuffix) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            ImmutableSet<ClassPath.ClassInfo> classesOfFlowConfigPackage = ClassPath.from(classLoader).getTopLevelClassesRecursive(flowConfigsPackage);
            Set<FlowConfiguration<? extends FlowEvent>> flowConfigurations = new OfflineStateGraphGenerator()
                    .gatherFlowConfigurationsFromPackage(classesOfFlowConfigPackage);
            new FlowChainConfigDotGraphGenerator(flowChainFactory).generateGraphAndSaveToFile(flowConfigurations, eventQueues, fileNameSuffix);
        } catch (IOException e) {
            LOGGER.warn("Failed to generate DOT graph for flow chain '{}' and file name suffix '{}'", flowChainFactory.getName(), fileNameSuffix, e);
        }
    }
}
