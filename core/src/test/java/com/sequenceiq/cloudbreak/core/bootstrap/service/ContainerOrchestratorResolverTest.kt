package com.sequenceiq.cloudbreak.core.bootstrap.service

import org.junit.Assert.assertNotNull

import java.util.HashMap

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.runners.MockitoJUnitRunner
import org.springframework.test.util.ReflectionTestUtils

import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator

@RunWith(MockitoJUnitRunner::class)
class ContainerOrchestratorResolverTest {

    @InjectMocks
    private val underTest: ContainerOrchestratorResolver? = null

    @Test
    @Throws(CloudbreakException::class)
    fun getOrchestratorWhenExist() {
        val map = HashMap<String, ContainerOrchestrator>()
        val testOneMockContainerOrchestrator = TestOneMockContainerOrchestrator()
        map.put(testOneMockContainerOrchestrator.name(), testOneMockContainerOrchestrator)
        val testTwoMockContainerOrchestrator = TestTwoMockContainerOrchestrator()
        map.put(testTwoMockContainerOrchestrator.name(), testTwoMockContainerOrchestrator)
        ReflectionTestUtils.setField(underTest, "containerOrchestrators", map)

        val containerOrchestrator = underTest!!.get("SWARM")
        assertNotNull(containerOrchestrator)
    }

    @Test(expected = CloudbreakException::class)
    @Throws(CloudbreakException::class)
    fun getOrchestratorWhenNotExist() {
        val map = HashMap<String, ContainerOrchestrator>()
        val testOneMockContainerOrchestrator = TestOneMockContainerOrchestrator()
        map.put(testOneMockContainerOrchestrator.name(), testOneMockContainerOrchestrator)
        val testTwoMockContainerOrchestrator = TestTwoMockContainerOrchestrator()
        map.put(testTwoMockContainerOrchestrator.name(), testTwoMockContainerOrchestrator)
        ReflectionTestUtils.setField(underTest, "containerOrchestrators", map)

        underTest!!.get("SWARM1")
    }

    internal inner class TestOneMockContainerOrchestrator : MockContainerOrchestrator() {
        override fun name(): String {
            return "SWARM"
        }
    }

    internal inner class TestTwoMockContainerOrchestrator : MockContainerOrchestrator() {
        override fun name(): String {
            return "MESOS"
        }
    }
}