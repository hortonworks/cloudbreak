package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

class ContainerOrchestratorResolverTest {

    private ContainerOrchestratorResolver underTest = new ContainerOrchestratorResolver();

    @Test
    void getOrchestratorWhenExist() throws CloudbreakException {
        Map<String, ContainerOrchestrator> map = new HashMap<>();
        TestOneMockContainerOrchestrator testOneMockContainerOrchestrator = new TestOneMockContainerOrchestrator();
        map.put(testOneMockContainerOrchestrator.name(), testOneMockContainerOrchestrator);
        TestTwoMockContainerOrchestrator testTwoMockContainerOrchestrator = new TestTwoMockContainerOrchestrator();
        map.put(testTwoMockContainerOrchestrator.name(), testTwoMockContainerOrchestrator);
        ReflectionTestUtils.setField(underTest, "containerOrchestrators", map);

        ContainerOrchestrator containerOrchestrator = underTest.get("YARN");
        assertNotNull(containerOrchestrator);
    }

    @Test
    void getOrchestratorWhenNotExist() throws CloudbreakException {
        Map<String, ContainerOrchestrator> map = new HashMap<>();
        TestOneMockContainerOrchestrator testOneMockContainerOrchestrator = new TestOneMockContainerOrchestrator();
        map.put(testOneMockContainerOrchestrator.name(), testOneMockContainerOrchestrator);
        TestTwoMockContainerOrchestrator testTwoMockContainerOrchestrator = new TestTwoMockContainerOrchestrator();
        map.put(testTwoMockContainerOrchestrator.name(), testTwoMockContainerOrchestrator);
        ReflectionTestUtils.setField(underTest, "containerOrchestrators", map);

        assertThrows(CloudbreakException.class, () -> underTest.get("SWARM1"), "ContainerOrchestrator not found: SWARM1");
    }

    private static class TestOneMockContainerOrchestrator extends MockContainerOrchestrator {
        @Override
        public String name() {
            return "YARN";
        }
    }

    private static class TestTwoMockContainerOrchestrator extends MockContainerOrchestrator {
        @Override
        public String name() {
            return "YARN";
        }
    }
}