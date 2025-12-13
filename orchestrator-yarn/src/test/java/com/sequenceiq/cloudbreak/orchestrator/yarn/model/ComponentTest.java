package com.sequenceiq.cloudbreak.orchestrator.yarn.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.Dependency;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.YarnComponent;

class ComponentTest {

    private static final String NAME = "testComp";

    private static final String LAUNCH_COMMAND = "/bin/foo";

    private static final int NUM_OF_CONTAINERS = 1;

    @Test
    void testLaunchCommand() {
        YarnComponent component = new YarnComponent();
        component.setLaunchCommand(LAUNCH_COMMAND);
        assertEquals(LAUNCH_COMMAND, component.getLaunchCommand());
    }

    @Test
    void testNumberOfContainers() {
        YarnComponent component = new YarnComponent();
        component.setNumberOfContainers(NUM_OF_CONTAINERS);
        assertEquals(NUM_OF_CONTAINERS, component.getNumberOfContainers());
    }

    @Test
    void testName() {
        YarnComponent component = new YarnComponent();
        component.setName(NAME);
        assertEquals(NAME, component.getName());
    }

    @Test
    void testDependencies() {
        Dependency dependency = new Dependency();
        YarnComponent component = new YarnComponent();
        List<Dependency> dependencies = new ArrayList<>();
        dependencies.add(dependency);
        component.setDependencies(dependencies);
        assertEquals(1L, component.getDependencies().size());
    }
}