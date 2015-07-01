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
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;

@RunWith(MockitoJUnitRunner.class)
public class ContainerOrchestratorResolverTest {

    @InjectMocks
    private ContainerOrchestratorResolver underTest;

    @Test
    public void getOrchestratorWhenExist() throws CloudbreakException {
        ReflectionTestUtils.setField(underTest, "containerOrchestratorName", "test1");
        Map<String, ContainerOrchestrator> map = new HashMap<>();
        TestOneMockContainerOrchestrator testOneMockContainerOrchestrator = new TestOneMockContainerOrchestrator();
        map.put(testOneMockContainerOrchestrator.name(), testOneMockContainerOrchestrator);
        TestTwoMockContainerOrchestrator testTwoMockContainerOrchestrator = new TestTwoMockContainerOrchestrator();
        map.put(testTwoMockContainerOrchestrator.name(), testTwoMockContainerOrchestrator);
        ReflectionTestUtils.setField(underTest, "containerOrchestrators", map);

        ContainerOrchestrator containerOrchestrator = underTest.get();
        assertNotNull(containerOrchestrator);
    }

    @Test(expected = CloudbreakException.class)
    public void getOrchestratorWhenNotExist() throws CloudbreakException {
        ReflectionTestUtils.setField(underTest, "containerOrchestratorName", "test3");
        Map<String, ContainerOrchestrator> map = new HashMap<>();
        TestOneMockContainerOrchestrator testOneMockContainerOrchestrator = new TestOneMockContainerOrchestrator();
        map.put(testOneMockContainerOrchestrator.name(), testOneMockContainerOrchestrator);
        TestTwoMockContainerOrchestrator testTwoMockContainerOrchestrator = new TestTwoMockContainerOrchestrator();
        map.put(testTwoMockContainerOrchestrator.name(), testTwoMockContainerOrchestrator);
        ReflectionTestUtils.setField(underTest, "containerOrchestrators", map);

        underTest.get();
    }

    class TestOneMockContainerOrchestrator extends MockContainerOrchestrator {
        @Override
        public String name() {
            return "test1";
        }
    }

    class TestTwoMockContainerOrchestrator extends MockContainerOrchestrator {
        @Override
        public String name() {
            return "test2";
        }
    }
}