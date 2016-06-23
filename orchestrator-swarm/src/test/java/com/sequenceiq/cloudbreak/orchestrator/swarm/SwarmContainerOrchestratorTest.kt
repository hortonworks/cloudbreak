package com.sequenceiq.cloudbreak.orchestrator.swarm


import com.sequenceiq.cloudbreak.orchestrator.swarm.OrchestratorTestUtil.createRunner
import com.sequenceiq.cloudbreak.orchestrator.swarm.OrchestratorTestUtil.exitCriteria
import com.sequenceiq.cloudbreak.orchestrator.swarm.OrchestratorTestUtil.exitCriteriaModel
import com.sequenceiq.cloudbreak.orchestrator.swarm.OrchestratorTestUtil.gatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.swarm.OrchestratorTestUtil.generateNodes
import com.sequenceiq.cloudbreak.orchestrator.swarm.OrchestratorTestUtil.parallelContainerRunner
import org.mockito.Matchers.any
import org.mockito.Matchers.anyMap
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mockito.Mockito.`when`
import java.util.concurrent.Callable
import java.util.concurrent.Future

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock
import org.mockito.runners.MockitoJUnitRunner
import org.mockito.stubbing.Answer

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.MunchausenBootstrap

@RunWith(MockitoJUnitRunner::class)
class SwarmContainerOrchestratorTest {

    private val underTest = SwarmContainerOrchestrator()
    private var underTestSpy: SwarmContainerOrchestrator? = null

    @Mock
    private val munchausenBootstrap: MunchausenBootstrap? = null

    @Mock
    private val future: Future<Boolean>? = null

    @SuppressWarnings("unchecked")
    @Before
    fun before() {
        underTest.init(parallelContainerRunner(), exitCriteria())
        underTestSpy = spy(underTest)
        doReturn(parallelContainerRunner()).`when`<SwarmContainerOrchestrator>(underTestSpy).parallelOrchestratorComponentRunner
        `when`(underTestSpy!!.runner(any<OrchestratorBootstrap>(OrchestratorBootstrap::class.java), any<ExitCriteria>(ExitCriteria::class.java), any<ExitCriteriaModel>(ExitCriteriaModel::class.java), anyMap())).thenAnswer { invocation ->
            val arguments = invocation.arguments
            val orchestratorBootstrap = arguments[ZERO] as OrchestratorBootstrap
            val exitCriteria = arguments[ONE] as ExitCriteria
            val exitCriteriaModel = arguments[TWO] as ExitCriteriaModel
            val map = arguments[THREE] as Map<String, String>
            createRunner(orchestratorBootstrap, exitCriteria, exitCriteriaModel, map)
        }
    }

    @Test
    @Throws(Exception::class)
    fun bootstrapClusterWhenEverythingWorksFine() {
        `when`<Boolean>(munchausenBootstrap!!.call()).thenReturn(true)
        doReturn(munchausenBootstrap).`when`<SwarmContainerOrchestrator>(underTestSpy).munchausenBootstrap(any<GatewayConfig>(GatewayConfig::class.java), any<String>(String::class.java), any<Array<String>>(Array<String>::class.java))

        underTestSpy!!.bootstrap(gatewayConfig(), ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), FIX_CONSUL_SERVER_COUNT,
                exitCriteriaModel())
    }

    @Test(expected = CloudbreakOrchestratorCancelledException::class)
    @Throws(Exception::class)
    fun bootstrapClusterWhenOrchestratorCancelled() {
        `when`<Boolean>(munchausenBootstrap!!.call()).thenThrow(CloudbreakOrchestratorCancelledException("cancelled"))
        doReturn(munchausenBootstrap).`when`<SwarmContainerOrchestrator>(underTestSpy).munchausenBootstrap(any<GatewayConfig>(GatewayConfig::class.java), any<String>(String::class.java), any<Array<String>>(Array<String>::class.java))

        underTestSpy!!.bootstrap(gatewayConfig(), ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), FIX_CONSUL_SERVER_COUNT,
                exitCriteriaModel())
    }

    @Test(expected = CloudbreakOrchestratorFailedException::class)
    @Throws(Exception::class)
    fun bootstrapClusterWhenOrchestratorFailed() {
        `when`<Boolean>(munchausenBootstrap!!.call()).thenThrow(CloudbreakOrchestratorFailedException("failed"))
        doReturn(munchausenBootstrap).`when`<SwarmContainerOrchestrator>(underTestSpy).munchausenBootstrap(any<GatewayConfig>(GatewayConfig::class.java), any<String>(String::class.java), any<Array<String>>(Array<String>::class.java))

        underTestSpy!!.bootstrap(gatewayConfig(), ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), FIX_CONSUL_SERVER_COUNT,
                exitCriteriaModel())
    }

    @Test(expected = CloudbreakOrchestratorFailedException::class)
    @Throws(Exception::class)
    fun bootstrapClusterWhenNullPointerOccurredAndOrchestratorFailedComes() {
        `when`<Boolean>(munchausenBootstrap!!.call()).thenThrow(NullPointerException("null"))
        doReturn(munchausenBootstrap).`when`<SwarmContainerOrchestrator>(underTestSpy).munchausenBootstrap(any<GatewayConfig>(GatewayConfig::class.java), any<String>(String::class.java), any<Array<String>>(Array<String>::class.java))

        underTestSpy!!.bootstrap(gatewayConfig(), ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), FIX_CONSUL_SERVER_COUNT,
                exitCriteriaModel())
    }

    @Test
    @Throws(Exception::class)
    fun bootstrapNewNodesInClusterWhenEverythingWorksFine() {
        `when`<Boolean>(munchausenBootstrap!!.call()).thenReturn(true)
        doReturn(munchausenBootstrap).`when`<SwarmContainerOrchestrator>(underTestSpy).munchausenNewNodeBootstrap(any<GatewayConfig>(GatewayConfig::class.java), any<String>(String::class.java), any<Array<String>>(Array<String>::class.java))

        underTestSpy!!.bootstrapNewNodes(gatewayConfig(), ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), exitCriteriaModel())
    }

    @Test(expected = CloudbreakOrchestratorCancelledException::class)
    @Throws(Exception::class)
    fun bootstrapNewNodesInClusterWhenOrchestratorCancelled() {
        `when`<Boolean>(munchausenBootstrap!!.call()).thenThrow(CloudbreakOrchestratorCancelledException("cancelled"))
        doReturn(munchausenBootstrap).`when`<SwarmContainerOrchestrator>(underTestSpy).munchausenNewNodeBootstrap(any<GatewayConfig>(GatewayConfig::class.java), any<String>(String::class.java), any<Array<String>>(Array<String>::class.java))

        underTestSpy!!.bootstrapNewNodes(gatewayConfig(), ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), exitCriteriaModel())
    }

    @Test(expected = CloudbreakOrchestratorFailedException::class)
    @Throws(Exception::class)
    fun bootstrapNewNodesInClusterWhenOrchestratorFailed() {
        `when`<Boolean>(munchausenBootstrap!!.call()).thenThrow(CloudbreakOrchestratorFailedException("failed"))
        doReturn(munchausenBootstrap).`when`<SwarmContainerOrchestrator>(underTestSpy).munchausenNewNodeBootstrap(any<GatewayConfig>(GatewayConfig::class.java), any<String>(String::class.java), any<Array<String>>(Array<String>::class.java))

        underTestSpy!!.bootstrapNewNodes(gatewayConfig(), ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), exitCriteriaModel())
    }

    @Test(expected = CloudbreakOrchestratorFailedException::class)
    @Throws(Exception::class)
    fun bootstrapNewNodesInClusterWhenNullPointerOccurredAndOrchestratorFailedComes() {
        `when`<Boolean>(munchausenBootstrap!!.call()).thenThrow(NullPointerException("null"))
        doReturn(munchausenBootstrap).`when`<SwarmContainerOrchestrator>(underTestSpy).munchausenNewNodeBootstrap(any<GatewayConfig>(GatewayConfig::class.java), any<String>(String::class.java), any<Array<String>>(Array<String>::class.java))

        underTestSpy!!.bootstrapNewNodes(gatewayConfig(), ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), exitCriteriaModel())
    }

    companion object {

        private val FIX_NODE_COUNT = 10
        private val FIX_CONSUL_SERVER_COUNT = 3
        private val ZERO = 0
        private val ONE = 1
        private val TWO = 2
        private val THREE = 3
    }
}