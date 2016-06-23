package com.sequenceiq.cloudbreak.orchestrator.salt

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.collection.IsCollectionWithSize.hasSize
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.mockito.Matchers.any
import org.mockito.Matchers.anyInt
import org.mockito.Matchers.eq
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.api.mockito.PowerMockito.verifyNew
import org.powermock.api.mockito.PowerMockito.verifyStatic
import org.powermock.api.mockito.PowerMockito.whenNew

import java.util.ArrayList
import java.util.HashSet
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

import com.google.common.collect.Sets
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrapRunner
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarConfig
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.PillarSave
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltBootstrap
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltCommandTracker
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltJobIdTracker
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.GrainAddRunner
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.HighStateRunner
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.SyncGrainsRunner
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel

@RunWith(PowerMockRunner::class)
@PrepareForTest(SaltOrchestrator::class, SaltStates::class)
class SaltOrchestratorTest {

    private var gatewayConfig: GatewayConfig? = null
    private var targets: MutableSet<Node>? = null
    private var exitCriteria: ExitCriteria? = null
    private var parallelOrchestratorComponentRunner: ParallelOrchestratorComponentRunner? = null
    private var saltConnector: SaltConnector? = null

    @Captor
    private val ipSet: ArgumentCaptor<Set<String>>? = null
    private var exitCriteriaModel: ExitCriteriaModel? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        gatewayConfig = GatewayConfig("1.1.1.1", "10.0.0.1", "10-0-0-1", 9443, "/certdir", "servercert", "clientcert", "clientkey")
        targets = HashSet<Node>()
        targets!!.add(Node("10.0.0.1", "1.1.1.1", "10-0-0-1.example.com"))
        targets!!.add(Node("10.0.0.2", "1.1.1.2", "10-0-0-2.example.com"))
        targets!!.add(Node("10.0.0.3", "1.1.1.3", "10-0-0-3.example.com"))

        saltConnector = mock<SaltConnector>(SaltConnector::class.java)
        PowerMockito.whenNew<SaltConnector>(SaltConnector::class.java).withAnyArguments().thenReturn(saltConnector)
        parallelOrchestratorComponentRunner = mock<ParallelOrchestratorComponentRunner>(ParallelOrchestratorComponentRunner::class.java)
        `when`(parallelOrchestratorComponentRunner!!.submit(any<Callable<Boolean>>())).thenReturn(CompletableFuture.completedFuture(true))
        exitCriteria = mock<ExitCriteria>(ExitCriteria::class.java)
        exitCriteriaModel = mock<ExitCriteriaModel>(ExitCriteriaModel::class.java)
    }

    @Test
    @Throws(Exception::class)
    fun bootstrapTest() {
        val saltOrchestrator = SaltOrchestrator()
        saltOrchestrator.init(parallelOrchestratorComponentRunner, exitCriteria)

        PowerMockito.whenNew<OrchestratorBootstrapRunner>(OrchestratorBootstrapRunner::class.java).withAnyArguments().thenReturn(mock<OrchestratorBootstrapRunner>(OrchestratorBootstrapRunner::class.java))
        PowerMockito.whenNew<SaltBootstrap>(SaltBootstrap::class.java).withAnyArguments().thenReturn(mock<SaltBootstrap>(SaltBootstrap::class.java))

        saltOrchestrator.bootstrap(gatewayConfig, targets, 0, exitCriteriaModel)

        verify<ParallelOrchestratorComponentRunner>(parallelOrchestratorComponentRunner, times(2)).submit(any<OrchestratorBootstrapRunner>(OrchestratorBootstrapRunner::class.java))

        verifyNew<Any>(OrchestratorBootstrapRunner::class.java, times(2)).withArguments(any<PillarSave>(PillarSave::class.java), eq<ExitCriteria>(exitCriteria), eq<ExitCriteriaModel>(exitCriteriaModel), any<Any>(), anyInt(), anyInt())
        verifyNew<Any>(OrchestratorBootstrapRunner::class.java, times(2)).withArguments(any<SaltBootstrap>(SaltBootstrap::class.java), eq<ExitCriteria>(exitCriteria), eq<ExitCriteriaModel>(exitCriteriaModel), any<Any>(), anyInt(), anyInt())
        verifyNew<Any>(SaltBootstrap::class.java, times(1)).withArguments(eq<SaltConnector>(saltConnector), eq<GatewayConfig>(gatewayConfig), eq<Set<Node>>(targets))
    }

    @Test
    @Throws(Exception::class)
    fun bootstrapNewNodesTest() {
        PowerMockito.whenNew<SaltBootstrap>(SaltBootstrap::class.java).withAnyArguments().thenReturn(mock<SaltBootstrap>(SaltBootstrap::class.java))
        PowerMockito.whenNew<OrchestratorBootstrapRunner>(OrchestratorBootstrapRunner::class.java).withAnyArguments().thenReturn(mock<OrchestratorBootstrapRunner>(OrchestratorBootstrapRunner::class.java))

        val saltOrchestrator = SaltOrchestrator()
        saltOrchestrator.init(parallelOrchestratorComponentRunner, exitCriteria)

        saltOrchestrator.bootstrapNewNodes(gatewayConfig, targets, exitCriteriaModel)

        verifyNew<Any>(OrchestratorBootstrapRunner::class.java, times(1)).withArguments(any<SaltBootstrap>(SaltBootstrap::class.java), eq<ExitCriteria>(exitCriteria), eq<ExitCriteriaModel>(exitCriteriaModel), any<Any>(), anyInt(), anyInt())
        verifyNew<Any>(SaltBootstrap::class.java, times(1)).withArguments(eq<SaltConnector>(saltConnector), eq<GatewayConfig>(gatewayConfig), eq<Set<Node>>(targets))
    }

    @Test
    @Throws(Exception::class)
    fun runServiceTest() {
        PowerMockito.whenNew<SaltBootstrap>(SaltBootstrap::class.java).withAnyArguments().thenReturn(mock<SaltBootstrap>(SaltBootstrap::class.java))
        PowerMockito.whenNew<OrchestratorBootstrapRunner>(OrchestratorBootstrapRunner::class.java).withAnyArguments().thenReturn(mock<OrchestratorBootstrapRunner>(OrchestratorBootstrapRunner::class.java))
        val pillarSave = mock<PillarSave>(PillarSave::class.java)
        whenNew<PillarSave>(PillarSave::class.java).withAnyArguments().thenReturn(pillarSave)

        val addRemoveGrainRunner = mock<GrainAddRunner>(GrainAddRunner::class.java)
        whenNew<GrainAddRunner>(GrainAddRunner::class.java).withAnyArguments().thenReturn(addRemoveGrainRunner)

        val roleCheckerSaltCommandTracker = mock<SaltCommandTracker>(SaltCommandTracker::class.java)
        whenNew<SaltCommandTracker>(SaltCommandTracker::class.java).withArguments(eq<SaltConnector>(saltConnector), eq(addRemoveGrainRunner)).thenReturn(roleCheckerSaltCommandTracker)

        val syncGrainsRunner = mock<SyncGrainsRunner>(SyncGrainsRunner::class.java)
        whenNew<SyncGrainsRunner>(SyncGrainsRunner::class.java).withAnyArguments().thenReturn(syncGrainsRunner)

        val syncGrainsCheckerSaltCommandTracker = mock<SaltCommandTracker>(SaltCommandTracker::class.java)
        whenNew<SaltCommandTracker>(SaltCommandTracker::class.java).withArguments(eq<SaltConnector>(saltConnector), eq(syncGrainsRunner)).thenReturn(syncGrainsCheckerSaltCommandTracker)

        val highStateRunner = mock<HighStateRunner>(HighStateRunner::class.java)
        whenNew<HighStateRunner>(HighStateRunner::class.java).withAnyArguments().thenReturn(highStateRunner)

        val saltJobIdTracker = mock<SaltJobIdTracker>(SaltJobIdTracker::class.java)
        whenNew<SaltJobIdTracker>(SaltJobIdTracker::class.java).withAnyArguments().thenReturn(saltJobIdTracker)

        val saltOrchestrator = SaltOrchestrator()
        saltOrchestrator.init(parallelOrchestratorComponentRunner, exitCriteria)

        val saltPillarConfig = SaltPillarConfig()
        saltOrchestrator.runService(gatewayConfig, targets, saltPillarConfig, exitCriteriaModel)

        // verify pillar save
        verifyNew<Any>(OrchestratorBootstrapRunner::class.java, times(1)).withArguments(eq(pillarSave), eq<ExitCriteria>(exitCriteria), eq<ExitCriteriaModel>(exitCriteriaModel), any<Any>(), anyInt(), anyInt())

        // verify ambari server role
        verifyNew<Any>(GrainAddRunner::class.java, times(1)).withArguments(eq(Sets.newHashSet(gatewayConfig!!.privateAddress)),
                eq<Set<Node>>(targets), eq("ambari_server"))

        // verify ambari agent role
        val allNodes = targets!!.stream().map(Function<Node, String> { it.getPrivateIp() }).collect(Collectors.toSet<String>())
        verifyNew<Any>(GrainAddRunner::class.java, times(1)).withArguments(eq<Set<String>>(allNodes),
                eq<Set<Node>>(targets), eq("ambari_agent"))
        // verify two role command (amabari server, ambari agent)
        verifyNew<Any>(SaltCommandTracker::class.java, times(2)).withArguments(eq<SaltConnector>(saltConnector), eq(addRemoveGrainRunner))
        // verify two OrchestratorBootstrapRunner call with rolechecker command tracker
        verifyNew<Any>(OrchestratorBootstrapRunner::class.java, times(2)).withArguments(eq(roleCheckerSaltCommandTracker), eq<ExitCriteria>(exitCriteria), eq<ExitCriteriaModel>(exitCriteriaModel), any<Any>(), anyInt(), anyInt())

        // verify syncgrains command
        verifyNew<Any>(SyncGrainsRunner::class.java, times(1)).withArguments(eq<Set<String>>(allNodes), eq<Set<Node>>(targets))
        verifyNew<Any>(SaltCommandTracker::class.java, times(1)).withArguments(eq<SaltConnector>(saltConnector), eq(syncGrainsRunner))
        verifyNew<Any>(OrchestratorBootstrapRunner::class.java, times(1)).withArguments(eq(syncGrainsCheckerSaltCommandTracker), eq<ExitCriteria>(exitCriteria), eq<ExitCriteriaModel>(exitCriteriaModel), any<Any>(), anyInt(), anyInt())

        // verify run new service
        verifyNew<Any>(HighStateRunner::class.java, atLeastOnce()).withArguments(eq<Set<String>>(allNodes),
                eq<Set<Node>>(targets))
        verifyNew<Any>(SaltJobIdTracker::class.java, atLeastOnce()).withArguments(eq<SaltConnector>(saltConnector), eq(highStateRunner))
    }

    @Test
    @Throws(Exception::class)
    fun tearDownTest() {
        val saltOrchestrator = SaltOrchestrator()
        saltOrchestrator.init(parallelOrchestratorComponentRunner, exitCriteria)

        val hostNames = ArrayList<String>()
        hostNames.add("10-0-0-1.example.com")
        hostNames.add("10-0-0-1.example.com")
        hostNames.add("10-0-0-1.example.com")

        mockStatic(SaltStates::class.java)
        SaltStates.removeMinions(eq<SaltConnector>(saltConnector), eq<List<String>>(hostNames))

        saltOrchestrator.tearDown(gatewayConfig, hostNames)

        verifyStatic()
        SaltStates.removeMinions(eq<SaltConnector>(saltConnector), eq<List<String>>(hostNames))
    }

    @Test
    @Throws(Exception::class)
    fun tearDownFailTest() {
        val saltOrchestrator = SaltOrchestrator()
        saltOrchestrator.init(parallelOrchestratorComponentRunner, exitCriteria)

        val hostNames = ArrayList<String>()
        hostNames.add("10-0-0-1.example.com")
        hostNames.add("10-0-0-1.example.com")
        hostNames.add("10-0-0-1.example.com")

        mockStatic(SaltStates::class.java)
        PowerMockito.`when`(SaltStates.removeMinions(eq<SaltConnector>(saltConnector), eq<List<String>>(hostNames))).thenThrow(NullPointerException())

        try {
            saltOrchestrator.tearDown(gatewayConfig, hostNames)
            fail()
        } catch (e: CloudbreakOrchestratorFailedException) {
            assertTrue(NullPointerException::class.java!!.isInstance(e.cause))
        }

    }

    @Test
    @Throws(Exception::class)
    fun getMissingNodesTest() {
        val saltOrchestrator = SaltOrchestrator()
        saltOrchestrator.init(parallelOrchestratorComponentRunner, exitCriteria)
        assertThat(saltOrchestrator.getMissingNodes(gatewayConfig, targets), hasSize<String>(0))
    }

    @Test
    @Throws(Exception::class)
    fun getAvailableNodesTest() {
        val saltOrchestrator = SaltOrchestrator()
        saltOrchestrator.init(parallelOrchestratorComponentRunner, exitCriteria)
        assertThat(saltOrchestrator.getAvailableNodes(gatewayConfig, targets), hasSize<String>(0))
    }

    @Test
    @Throws(Exception::class)
    fun isBootstrapApiAvailableTest() {
        val saltOrchestrator = SaltOrchestrator()
        saltOrchestrator.init(parallelOrchestratorComponentRunner, exitCriteria)

        val response = GenericResponse()
        response.statusCode = 200
        `when`(saltConnector!!.health()).thenReturn(response)

        val bootstrapApiAvailable = saltOrchestrator.isBootstrapApiAvailable(gatewayConfig)
        assertTrue(bootstrapApiAvailable)
    }

    @Test
    @Throws(Exception::class)
    fun isBootstrapApiAvailableFailTest() {
        val saltOrchestrator = SaltOrchestrator()
        saltOrchestrator.init(parallelOrchestratorComponentRunner, exitCriteria)

        val response = GenericResponse()
        response.statusCode = 404
        `when`(saltConnector!!.health()).thenReturn(response)

        val bootstrapApiAvailable = saltOrchestrator.isBootstrapApiAvailable(gatewayConfig)
        assertFalse(bootstrapApiAvailable)
    }
}