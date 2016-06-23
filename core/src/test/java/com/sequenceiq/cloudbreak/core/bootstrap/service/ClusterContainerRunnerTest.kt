package com.sequenceiq.cloudbreak.core.bootstrap.service

import org.mockito.Matchers.anyInt
import org.mockito.Matchers.anyList
import org.mockito.Matchers.anyLong
import org.mockito.Matchers.anyString
import org.mockito.Mockito.`when`

import java.util.ArrayList
import java.util.HashSet

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner
import org.springframework.test.util.ReflectionTestUtils

import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException
import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ClusterContainerRunner
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerConstraintFactory
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Container
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.repository.HostGroupRepository
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.cluster.ContainerService

@RunWith(MockitoJUnitRunner::class)
class ClusterContainerRunnerTest {
    @Mock
    private val stackRepository: StackRepository? = null

    @Mock
    private val containerOrchestratorResolver: ContainerOrchestratorResolver? = null

    @Mock
    private val tlsSecurityService: TlsSecurityService? = null

    @Mock
    private val containerService: ContainerService? = null

    @Mock
    private val instanceMetaDataRepository: InstanceMetaDataRepository? = null

    @Mock
    private val clusterService: ClusterService? = null

    @Mock
    private val hostGroupRepository: HostGroupRepository? = null

    @Mock
    private val mockContainerOrchestrator: MockContainerOrchestrator? = null

    @Mock
    private val cancelledMockContainerOrchestrator: CancelledMockContainerOrchestrator? = null

    @Mock
    private val failedMockContainerOrchestrator: FailedMockContainerOrchestrator? = null

    @Mock
    private val containerConfigService: ContainerConfigService? = null

    @Mock
    private val constraintFactory: ContainerConstraintFactory? = null

    @InjectMocks
    private val underTest: ClusterContainerRunner? = null

    @Before
    @Throws(CloudbreakException::class)
    fun setUp() {
        ReflectionTestUtils.setField(containerConfigService, "ambariAgent", "sequence/testcont:0.1.1")
        ReflectionTestUtils.setField(containerConfigService, "ambariServer", "sequence/testcont:0.1.1")
        ReflectionTestUtils.setField(containerConfigService, "registratorDockerImageName", "sequence/testcont:0.1.1")
        ReflectionTestUtils.setField(containerConfigService, "consulWatchPlugnDockerImageName", "sequence/testcont:0.1.1")
        ReflectionTestUtils.setField(containerConfigService, "postgresDockerImageName", "sequence/testcont:0.1.1")
        ReflectionTestUtils.setField(containerConfigService, "kerberosDockerImageName", "sequence/testcont:0.1.1")
        ReflectionTestUtils.setField(containerConfigService, "logrotateDockerImageName", "sequence/testcont:0.1.1")
    }

    @Test(expected = CloudbreakException::class)
    @Throws(CloudbreakException::class, CloudbreakOrchestratorFailedException::class, CloudbreakOrchestratorCancelledException::class)
    fun runNewNodesClusterContainersWhenContainerRunnerFailed() {
        val stack = TestUtil.stack()
        val cluster = TestUtil.cluster(TestUtil.blueprint(), stack, 1L)
        stack.cluster = cluster
        val hostGroupAdjustment = HostGroupAdjustmentJson()
        hostGroupAdjustment.hostGroup = "agent"
        `when`(containerOrchestratorResolver!!.get(anyString())).thenReturn(FailedMockContainerOrchestrator())
        `when`(clusterService!!.retrieveClusterByStackId(anyLong())).thenReturn(cluster)

        val containers = HashSet<Container>()

        val ambariServer = Container()
        ambariServer.name = "server"
        ambariServer.image = DockerContainer.AMBARI_SERVER.name
        ambariServer.host = "hostname-1"
        ambariServer.containerId = "1"

        val ambariAgent = Container()
        ambariAgent.name = "agent"
        ambariAgent.image = DockerContainer.AMBARI_AGENT.name
        ambariAgent.host = "hostname-2"
        ambariAgent.containerId = "1"

        containers.add(ambariAgent)
        containers.add(ambariServer)

        `when`(containerService!!.findContainersInCluster(anyLong())).thenReturn(containers)
        `when`(hostGroupRepository!!.findHostGroupInClusterByName(anyLong(), anyString())).thenReturn(TestUtil.hostGroup())
        `when`(stackRepository!!.findOneWithLists(anyLong())).thenReturn(stack)
        `when`(tlsSecurityService!!.buildGatewayConfig(anyLong(), anyString(), anyInt(), anyString(), anyString())).thenReturn(GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"))
        `when`(instanceMetaDataRepository!!.findAliveInstancesInInstanceGroup(anyLong())).thenReturn(ArrayList<InstanceMetaData>())
        `when`(containerService.save(anyList())).thenReturn(ArrayList<Container>())
        `when`(constraintFactory!!.getAmbariAgentConstraint(ambariServer.host, null, stack.cloudPlatform(),
                TestUtil.hostGroup(), hostGroupAdjustment.scalingAdjustment, ArrayList<String>())).thenReturn(ContainerConstraint.Builder().build())
        underTest!!.addClusterContainers(stack.id, hostGroupAdjustment.hostGroup, hostGroupAdjustment.scalingAdjustment)
    }

    @Test(expected = CancellationException::class)
    @Throws(CloudbreakException::class, CloudbreakOrchestratorFailedException::class, CloudbreakOrchestratorCancelledException::class)
    fun runNewNodesClusterContainersWhenContainerRunnerCancelled() {
        val stack = TestUtil.stack()
        val cluster = TestUtil.cluster(TestUtil.blueprint(), stack, 1L)
        stack.cluster = cluster
        val hostGroupAdjustment = HostGroupAdjustmentJson()
        hostGroupAdjustment.hostGroup = "agent"
        `when`(containerOrchestratorResolver!!.get(anyString())).thenReturn(CancelledMockContainerOrchestrator())
        `when`(stackRepository!!.findOneWithLists(anyLong())).thenReturn(stack)
        `when`(tlsSecurityService!!.buildGatewayConfig(anyLong(), anyString(), anyInt(), anyString(), anyString())).thenReturn(GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"))
        `when`(clusterService!!.retrieveClusterByStackId(anyLong())).thenReturn(cluster)
        `when`(hostGroupRepository!!.findHostGroupInClusterByName(anyLong(), anyString())).thenReturn(TestUtil.hostGroup())

        val containers = HashSet<Container>()

        val ambariServer = Container()
        ambariServer.name = "server"
        ambariServer.image = DockerContainer.AMBARI_SERVER.name
        ambariServer.host = "hostname-1"
        ambariServer.containerId = "1"

        val ambariAgent = Container()
        ambariAgent.name = "agent"
        ambariAgent.image = DockerContainer.AMBARI_AGENT.name
        ambariAgent.host = "hostname-2"
        ambariAgent.containerId = "1"

        containers.add(ambariAgent)
        containers.add(ambariServer)

        `when`(containerService!!.findContainersInCluster(anyLong())).thenReturn(containers)

        underTest!!.addClusterContainers(stack.id, hostGroupAdjustment.hostGroup, hostGroupAdjustment.scalingAdjustment)
    }
}
