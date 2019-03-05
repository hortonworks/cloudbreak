package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@RunWith(MockitoJUnitRunner.class)
public class ContainerOrchestratorResolverTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

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

        ContainerOrchestrator containerOrchestrator = underTest.get("YARN");
        assertNotNull(containerOrchestrator);
    }

    @Test
    public void getOrchestratorWhenNotExist() throws CloudbreakException {
        Map<String, ContainerOrchestrator> map = new HashMap<>();
        TestOneMockContainerOrchestrator testOneMockContainerOrchestrator = new TestOneMockContainerOrchestrator();
        map.put(testOneMockContainerOrchestrator.name(), testOneMockContainerOrchestrator);
        TestTwoMockContainerOrchestrator testTwoMockContainerOrchestrator = new TestTwoMockContainerOrchestrator();
        map.put(testTwoMockContainerOrchestrator.name(), testTwoMockContainerOrchestrator);
        ReflectionTestUtils.setField(underTest, "containerOrchestrators", map);
        thrown.expect(CloudbreakException.class);
        thrown.expectMessage("ContainerOrchestrator not found: SWARM1");

        underTest.get("SWARM1");
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