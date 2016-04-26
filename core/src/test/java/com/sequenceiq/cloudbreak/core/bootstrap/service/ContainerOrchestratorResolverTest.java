package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;

@RunWith(MockitoJUnitRunner.class)
public class ContainerOrchestratorResolverTest {

    @InjectMocks
    private ContainerOrchestratorResolver underTest;

    @Test
    public void getOrchestratorWhenExist() throws CloudbreakException {
        Map<String, ContainerOrchestrator> map = new HashMap<>();
        TestOneMockContainerOrchestrator testOneMockContainerOrchestrator = new TestOneMockContainerOrchestrator();
        map.put(testOneMockContainerOrchestrator.name(), testOneMockContainerOrchestrator);
        TestTwoMockContainerOrchestrator testTwoMockContainerOrchestrator = new TestTwoMockContainerOrchestrator();
        map.put(testTwoMockContainerOrchestrator.name(), testTwoMockContainerOrchestrator);
        ReflectionTestUtils.setField(underTest, "containerOrchestrators", map);

        ContainerOrchestrator containerOrchestrator = underTest.get("SWARM");
        assertNotNull(containerOrchestrator);
    }

    @Test(expected = CloudbreakException.class)
    public void getOrchestratorWhenNotExist() throws CloudbreakException {
        Map<String, ContainerOrchestrator> map = new HashMap<>();
        TestOneMockContainerOrchestrator testOneMockContainerOrchestrator = new TestOneMockContainerOrchestrator();
        map.put(testOneMockContainerOrchestrator.name(), testOneMockContainerOrchestrator);
        TestTwoMockContainerOrchestrator testTwoMockContainerOrchestrator = new TestTwoMockContainerOrchestrator();
        map.put(testTwoMockContainerOrchestrator.name(), testTwoMockContainerOrchestrator);
        ReflectionTestUtils.setField(underTest, "containerOrchestrators", map);

        underTest.get("SWARM1");
    }

    class TestOneMockContainerOrchestrator extends MockContainerOrchestrator {
        @Override
        public String name() {
            return "SWARM";
        }
    }

    class TestTwoMockContainerOrchestrator extends MockContainerOrchestrator {
        @Override
        public String name() {
            return "MESOS";
        }
    }
}