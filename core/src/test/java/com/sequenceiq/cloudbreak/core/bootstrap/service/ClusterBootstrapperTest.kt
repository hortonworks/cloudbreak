package com.sequenceiq.cloudbreak.core.bootstrap.service

import org.mockito.Matchers.any
import org.mockito.Matchers.anyInt
import org.mockito.Matchers.anyLong
import org.mockito.Matchers.anySet
import org.mockito.Matchers.anyString
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import java.util.HashSet

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner
import org.springframework.test.util.ReflectionTestUtils

import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException
import com.sequenceiq.cloudbreak.common.type.CloudConstants
import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerBootstrapApiCheckerTask
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerClusterAvailabilityCheckerTask
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostBootstrapApiCheckerTask
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostClusterAvailabilityCheckerTask
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.context.ContainerBootstrapApiContext
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.context.ContainerOrchestratorClusterContext
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.context.HostBootstrapApiContext
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.context.HostOrchestratorClusterContext
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Orchestrator
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel
import com.sequenceiq.cloudbreak.repository.OrchestratorRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.service.PollingResult
import com.sequenceiq.cloudbreak.service.PollingService
import com.sequenceiq.cloudbreak.service.StatusCheckerTask
import com.sequenceiq.cloudbreak.service.TlsSecurityService

@RunWith(MockitoJUnitRunner::class)
class ClusterBootstrapperTest {

    @Mock
    private val stackRepository: StackRepository? = null

    @Mock
    private val orchestratorRepository: OrchestratorRepository? = null

    @Mock
    private val containerBootstrapApiPollingService: PollingService<ContainerBootstrapApiContext>? = null

    @Mock
    private val hostBootstrapApiPollingService: PollingService<HostBootstrapApiContext>? = null

    @Mock
    private val containerBootstrapApiCheckerTask: ContainerBootstrapApiCheckerTask? = null

    @Mock
    private val hostBootstrapApiCheckerTask: HostBootstrapApiCheckerTask? = null

    @Mock
    private val containerClusterAvailabilityPollingService: PollingService<ContainerOrchestratorClusterContext>? = null

    @Mock
    private val hostClusterAvailabilityPollingService: PollingService<HostOrchestratorClusterContext>? = null

    @Mock
    private val containerClusterAvailabilityCheckerTask: ContainerClusterAvailabilityCheckerTask? = null

    @Mock
    private val hostClusterAvailabilityCheckerTask: HostClusterAvailabilityCheckerTask? = null

    @Mock
    private val clusterBootstrapperErrorHandler: ClusterBootstrapperErrorHandler? = null

    @Mock
    private val containerOrchestratorResolver: ContainerOrchestratorResolver? = null

    @Mock
    private val hostOrchestratorResolver: HostOrchestratorResolver? = null

    @Mock
    private val tlsSecurityService: TlsSecurityService? = null

    @Mock
    private val containerConfigService: ContainerConfigService? = null

    @Mock
    private val orchestratorTypeResolver: OrchestratorTypeResolver? = null

    @InjectMocks
    private val underTest: ClusterBootstrapper? = null

    @Before
    @Throws(CloudbreakException::class)
    fun setUp() {
        `when`(orchestratorTypeResolver!!.resolveType(anyString())).thenReturn(OrchestratorType.CONTAINER)
        ReflectionTestUtils.setField(containerConfigService, "munchausenImageName", "sequence/testcont:0.1.1")
    }

    @Test
    @Throws(CloudbreakException::class, CloudbreakOrchestratorFailedException::class)
    fun bootstrapClusterWhenEverythingWorksNormally() {
        val stack = TestUtil.stack()

        `when`(tlsSecurityService!!.buildGatewayConfig(anyLong(), anyString(), anyInt(), anyString(), anyString())).thenReturn(GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"))
        `when`(containerOrchestratorResolver!!.get("SWARM")).thenReturn(MockContainerOrchestrator())
        `when`(containerBootstrapApiPollingService!!.pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java), any<ContainerBootstrapApiContext>(ContainerBootstrapApiContext::class.java), anyInt(),
                anyInt())).thenReturn(PollingResult.SUCCESS)
        `when`(containerClusterAvailabilityPollingService!!.pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java),
                any<ContainerOrchestratorClusterContext>(ContainerOrchestratorClusterContext::class.java), anyInt(), anyInt())).thenReturn(PollingResult.SUCCESS)
        doNothing().`when`<ClusterBootstrapperErrorHandler>(clusterBootstrapperErrorHandler).terminateFailedNodes(any<HostOrchestrator>(HostOrchestrator::class.java), any<ContainerOrchestrator>(ContainerOrchestrator::class.java), any<Stack>(Stack::class.java),
                any<GatewayConfig>(GatewayConfig::class.java), any<Set<Any>>(Set<Any>::class.java))
        `when`(orchestratorRepository!!.save(any<Orchestrator>(Orchestrator::class.java))).thenReturn(Orchestrator())
        underTest!!.bootstrapContainers(stack)

        verify(tlsSecurityService, times(1)).buildGatewayConfig(anyLong(), anyString(), anyInt(), anyString(), anyString())
        verify<ClusterBootstrapperErrorHandler>(clusterBootstrapperErrorHandler, times(0)).terminateFailedNodes(any<HostOrchestrator>(HostOrchestrator::class.java), any<ContainerOrchestrator>(ContainerOrchestrator::class.java), any<Stack>(Stack::class.java), any<GatewayConfig>(GatewayConfig::class.java), anySet())
        verify(containerBootstrapApiPollingService, times(1)).pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java),
                any<ContainerBootstrapApiContext>(ContainerBootstrapApiContext::class.java), anyInt(),
                anyInt())
        verify(containerClusterAvailabilityPollingService, times(1)).pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java),
                any<ContainerOrchestratorClusterContext>(ContainerOrchestratorClusterContext::class.java), anyInt(), anyInt())
    }

    @Test
    @Throws(CloudbreakException::class, CloudbreakOrchestratorFailedException::class)
    fun bootstrapClusterWhenTimeOutComesInClusterAvailabilityPoller() {
        val stack = TestUtil.stack()

        `when`(tlsSecurityService!!.buildGatewayConfig(anyLong(), anyString(), anyInt(), anyString(), anyString())).thenReturn(GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"))
        `when`(containerOrchestratorResolver!!.get("SWARM")).thenReturn(MockContainerOrchestrator())
        `when`(containerBootstrapApiPollingService!!.pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java), any<ContainerBootstrapApiContext>(ContainerBootstrapApiContext::class.java), anyInt(),
                anyInt())).thenReturn(PollingResult.SUCCESS)
        `when`(containerClusterAvailabilityPollingService!!.pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java),
                any<ContainerOrchestratorClusterContext>(ContainerOrchestratorClusterContext::class.java), anyInt(), anyInt())).thenReturn(PollingResult.TIMEOUT)
        doNothing().`when`<ClusterBootstrapperErrorHandler>(clusterBootstrapperErrorHandler).terminateFailedNodes(any<HostOrchestrator>(HostOrchestrator::class.java), any<ContainerOrchestrator>(ContainerOrchestrator::class.java), any<Stack>(Stack::class.java),
                any<GatewayConfig>(GatewayConfig::class.java), any<Set<Any>>(Set<Any>::class.java))

        underTest!!.bootstrapContainers(stack)

        verify(tlsSecurityService, times(1)).buildGatewayConfig(anyLong(), anyString(), anyInt(), anyString(), anyString())
        verify<ClusterBootstrapperErrorHandler>(clusterBootstrapperErrorHandler, times(1)).terminateFailedNodes(any<HostOrchestrator>(HostOrchestrator::class.java), any<ContainerOrchestrator>(ContainerOrchestrator::class.java), any<Stack>(Stack::class.java), any<GatewayConfig>(GatewayConfig::class.java), anySet())
        verify(containerBootstrapApiPollingService, times(1)).pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java),
                any<ContainerBootstrapApiContext>(ContainerBootstrapApiContext::class.java), anyInt(),
                anyInt())
        verify(containerClusterAvailabilityPollingService, times(1)).pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java),
                any<ContainerOrchestratorClusterContext>(ContainerOrchestratorClusterContext::class.java), anyInt(), anyInt())
    }

    @Test(expected = CancellationException::class)
    @Throws(CloudbreakException::class, CloudbreakOrchestratorFailedException::class)
    fun bootstrapClusterWhenOrchestratorDropCancelledException() {
        val stack = TestUtil.stack()

        `when`(tlsSecurityService!!.buildGatewayConfig(anyLong(), anyString(), anyInt(), anyString(), anyString())).thenReturn(GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"))
        `when`(containerOrchestratorResolver!!.get("SWARM")).thenReturn(CancelledMockContainerOrchestrator())
        `when`(containerBootstrapApiPollingService!!.pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java), any<ContainerBootstrapApiContext>(ContainerBootstrapApiContext::class.java), anyInt(),
                anyInt())).thenReturn(PollingResult.SUCCESS)
        `when`(containerClusterAvailabilityPollingService!!.pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java),
                any<ContainerOrchestratorClusterContext>(ContainerOrchestratorClusterContext::class.java), anyInt(), anyInt())).thenReturn(PollingResult.SUCCESS)
        doNothing().`when`<ClusterBootstrapperErrorHandler>(clusterBootstrapperErrorHandler).terminateFailedNodes(any<HostOrchestrator>(HostOrchestrator::class.java), any<ContainerOrchestrator>(ContainerOrchestrator::class.java), any<Stack>(Stack::class.java),
                any<GatewayConfig>(GatewayConfig::class.java), any<Set<Any>>(Set<Any>::class.java))

        underTest!!.bootstrapContainers(stack)
    }

    @Test(expected = CloudbreakException::class)
    @Throws(CloudbreakException::class, CloudbreakOrchestratorFailedException::class)
    fun bootstrapClusterWhenOrchestratorDropFailedException() {
        val stack = TestUtil.stack()

        `when`(tlsSecurityService!!.buildGatewayConfig(anyLong(), anyString(), anyInt(), anyString(), anyString())).thenReturn(GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"))
        `when`(containerOrchestratorResolver!!.get("SWARM")).thenReturn(FailedMockContainerOrchestrator())
        `when`(containerBootstrapApiPollingService!!.pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java), any<ContainerBootstrapApiContext>(ContainerBootstrapApiContext::class.java), anyInt(),
                anyInt())).thenReturn(PollingResult.SUCCESS)
        `when`(containerClusterAvailabilityPollingService!!.pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java),
                any<ContainerOrchestratorClusterContext>(ContainerOrchestratorClusterContext::class.java), anyInt(), anyInt())).thenReturn(PollingResult.SUCCESS)
        doNothing().`when`<ClusterBootstrapperErrorHandler>(clusterBootstrapperErrorHandler).terminateFailedNodes(any<HostOrchestrator>(HostOrchestrator::class.java), any<ContainerOrchestrator>(ContainerOrchestrator::class.java), any<Stack>(Stack::class.java),
                any<GatewayConfig>(GatewayConfig::class.java), any<Set<Any>>(Set<Any>::class.java))

        underTest!!.bootstrapContainers(stack)
    }

    @Test
    @Throws(CloudbreakException::class, CloudbreakOrchestratorFailedException::class)
    fun bootstrapClusterWhenEverythingWorksNormallyWithMoreBootstrapSegment() {
        val stack = TestUtil.stack()

        `when`(tlsSecurityService!!.buildGatewayConfig(anyLong(), anyString(), anyInt(), anyString(), anyString())).thenReturn(GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"))
        `when`(containerOrchestratorResolver!!.get("SWARM")).thenReturn(TwoLengthMockContainerOrchestrator())
        `when`(containerBootstrapApiPollingService!!.pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java), any<ContainerBootstrapApiContext>(ContainerBootstrapApiContext::class.java), anyInt(),
                anyInt())).thenReturn(PollingResult.SUCCESS)
        `when`(containerClusterAvailabilityPollingService!!.pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java),
                any<ContainerOrchestratorClusterContext>(ContainerOrchestratorClusterContext::class.java), anyInt(), anyInt())).thenReturn(PollingResult.SUCCESS)
        doNothing().`when`<ClusterBootstrapperErrorHandler>(clusterBootstrapperErrorHandler).terminateFailedNodes(any<HostOrchestrator>(HostOrchestrator::class.java), any<ContainerOrchestrator>(ContainerOrchestrator::class.java), any<Stack>(Stack::class.java),
                any<GatewayConfig>(GatewayConfig::class.java), any<Set<Any>>(Set<Any>::class.java))

        underTest!!.bootstrapContainers(stack)

        verify<ClusterBootstrapperErrorHandler>(clusterBootstrapperErrorHandler, times(0)).terminateFailedNodes(any<HostOrchestrator>(HostOrchestrator::class.java), any<ContainerOrchestrator>(ContainerOrchestrator::class.java), any<Stack>(Stack::class.java), any<GatewayConfig>(GatewayConfig::class.java), anySet())
        verify(tlsSecurityService, times(1)).buildGatewayConfig(anyLong(), anyString(), anyInt(), anyString(), anyString())
        verify(containerBootstrapApiPollingService, times(1)).pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java),
                any<ContainerBootstrapApiContext>(ContainerBootstrapApiContext::class.java), anyInt(), anyInt())
        verify(containerClusterAvailabilityPollingService, times(3)).pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java),
                any<ContainerOrchestratorClusterContext>(ContainerOrchestratorClusterContext::class.java), anyInt(), anyInt())
    }

    @Test
    @Throws(CloudbreakException::class, CloudbreakOrchestratorFailedException::class)
    fun bootstrapNewNodesInClusterWhenEverythingWorksNormally() {
        val stack = TestUtil.stack()

        `when`(stackRepository!!.findOneWithLists(anyLong())).thenReturn(stack)
        `when`(tlsSecurityService!!.buildGatewayConfig(anyLong(), anyString(), anyInt(), anyString(), anyString())).thenReturn(GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"))
        `when`(containerOrchestratorResolver!!.get("SWARM")).thenReturn(MockContainerOrchestrator())
        `when`(containerBootstrapApiPollingService!!.pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java), any<ContainerBootstrapApiContext>(ContainerBootstrapApiContext::class.java), anyInt(),
                anyInt())).thenReturn(PollingResult.SUCCESS)
        `when`(containerClusterAvailabilityPollingService!!.pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java),
                any<ContainerOrchestratorClusterContext>(ContainerOrchestratorClusterContext::class.java), anyInt(), anyInt())).thenReturn(PollingResult.SUCCESS)
        doNothing().`when`<ClusterBootstrapperErrorHandler>(clusterBootstrapperErrorHandler).terminateFailedNodes(any<HostOrchestrator>(HostOrchestrator::class.java), any<ContainerOrchestrator>(ContainerOrchestrator::class.java), any<Stack>(Stack::class.java),
                any<GatewayConfig>(GatewayConfig::class.java), any<Set<Any>>(Set<Any>::class.java))

        underTest!!.bootstrapNewNodes(stack.id, getPrivateIps(stack))

        verify<ClusterBootstrapperErrorHandler>(clusterBootstrapperErrorHandler, times(0)).terminateFailedNodes(any<HostOrchestrator>(HostOrchestrator::class.java), any<ContainerOrchestrator>(ContainerOrchestrator::class.java), any<Stack>(Stack::class.java), any<GatewayConfig>(GatewayConfig::class.java), anySet())
        verify(tlsSecurityService, times(1)).buildGatewayConfig(anyLong(), anyString(), anyInt(), anyString(), anyString())
        verify(containerClusterAvailabilityPollingService, times(2)).pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java),
                any<ContainerOrchestratorClusterContext>(ContainerOrchestratorClusterContext::class.java), anyInt(), anyInt())
    }

    @Test
    @Throws(CloudbreakException::class, CloudbreakOrchestratorFailedException::class)
    fun bootstrapNewNodesInClusterWhenBootstrapHappeningInTwoSegments() {
        val stack = TestUtil.stack()

        `when`(stackRepository!!.findOneWithLists(anyLong())).thenReturn(stack)
        `when`(tlsSecurityService!!.buildGatewayConfig(anyLong(), anyString(), anyInt(), anyString(), anyString())).thenReturn(GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"))
        `when`(containerOrchestratorResolver!!.get("SWARM")).thenReturn(TwoLengthMockContainerOrchestrator())
        `when`(containerBootstrapApiPollingService!!.pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java), any<ContainerBootstrapApiContext>(ContainerBootstrapApiContext::class.java), anyInt(),
                anyInt())).thenReturn(PollingResult.SUCCESS)
        `when`(containerClusterAvailabilityPollingService!!.pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java),
                any<ContainerOrchestratorClusterContext>(ContainerOrchestratorClusterContext::class.java), anyInt(), anyInt())).thenReturn(PollingResult.SUCCESS)
        doNothing().`when`<ClusterBootstrapperErrorHandler>(clusterBootstrapperErrorHandler).terminateFailedNodes(any<HostOrchestrator>(HostOrchestrator::class.java), any<ContainerOrchestrator>(ContainerOrchestrator::class.java), any<Stack>(Stack::class.java),
                any<GatewayConfig>(GatewayConfig::class.java), any<Set<Any>>(Set<Any>::class.java))

        underTest!!.bootstrapNewNodes(stack.id, getPrivateIps(stack))
        verify<ClusterBootstrapperErrorHandler>(clusterBootstrapperErrorHandler, times(0)).terminateFailedNodes(any<HostOrchestrator>(HostOrchestrator::class.java), any<ContainerOrchestrator>(ContainerOrchestrator::class.java), any<Stack>(Stack::class.java), any<GatewayConfig>(GatewayConfig::class.java), anySet())
        verify(tlsSecurityService, times(1)).buildGatewayConfig(anyLong(), anyString(), anyInt(), anyString(), anyString())
        verify(containerClusterAvailabilityPollingService, times(3)).pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java),
                any<ContainerOrchestratorClusterContext>(ContainerOrchestratorClusterContext::class.java), anyInt(), anyInt())
    }

    @Test
    @Throws(CloudbreakException::class, CloudbreakOrchestratorFailedException::class)
    fun bootstrapNewNodesInClusterWhenClusterAvailabilityDropTimeout() {
        val stack = TestUtil.stack()

        `when`(stackRepository!!.findOneWithLists(anyLong())).thenReturn(stack)
        `when`(tlsSecurityService!!.buildGatewayConfig(anyLong(), anyString(), anyInt(), anyString(), anyString())).thenReturn(GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"))
        `when`(containerOrchestratorResolver!!.get("SWARM")).thenReturn(MockContainerOrchestrator())
        `when`(containerBootstrapApiPollingService!!.pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java), any<ContainerBootstrapApiContext>(ContainerBootstrapApiContext::class.java), anyInt(),
                anyInt())).thenReturn(PollingResult.SUCCESS)
        `when`(containerClusterAvailabilityPollingService!!.pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java),
                any<ContainerOrchestratorClusterContext>(ContainerOrchestratorClusterContext::class.java), anyInt(), anyInt())).thenReturn(PollingResult.TIMEOUT)
        doNothing().`when`<ClusterBootstrapperErrorHandler>(clusterBootstrapperErrorHandler).terminateFailedNodes(any<HostOrchestrator>(HostOrchestrator::class.java), any<ContainerOrchestrator>(ContainerOrchestrator::class.java), any<Stack>(Stack::class.java),
                any<GatewayConfig>(GatewayConfig::class.java), any<Set<Any>>(Set<Any>::class.java))

        underTest!!.bootstrapNewNodes(stack.id, getPrivateIps(stack))

        verify<ClusterBootstrapperErrorHandler>(clusterBootstrapperErrorHandler, times(1)).terminateFailedNodes(any<HostOrchestrator>(HostOrchestrator::class.java), any<ContainerOrchestrator>(ContainerOrchestrator::class.java), any<Stack>(Stack::class.java), any<GatewayConfig>(GatewayConfig::class.java), anySet())
        verify(tlsSecurityService, times(1)).buildGatewayConfig(anyLong(), anyString(), anyInt(), anyString(), anyString())
        verify(containerClusterAvailabilityPollingService, times(2)).pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java),
                any<ContainerOrchestratorClusterContext>(ContainerOrchestratorClusterContext::class.java), anyInt(), anyInt())
    }

    @Test(expected = CancellationException::class)
    @Throws(CloudbreakException::class, CloudbreakOrchestratorFailedException::class)
    fun bootstrapNewNodesInClusterWhenOrchestratorDropCancelledException() {
        val stack = TestUtil.stack()

        `when`(stackRepository!!.findOneWithLists(anyLong())).thenReturn(stack)
        `when`(tlsSecurityService!!.buildGatewayConfig(anyLong(), anyString(), anyInt(), anyString(), anyString())).thenReturn(GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"))
        `when`(containerOrchestratorResolver!!.get("SWARM")).thenReturn(CancelledNewNodesMockContainerOrchestrator())
        `when`(containerBootstrapApiPollingService!!.pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java), any<ContainerBootstrapApiContext>(ContainerBootstrapApiContext::class.java), anyInt(),
                anyInt())).thenReturn(PollingResult.SUCCESS)
        `when`(containerClusterAvailabilityPollingService!!.pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java),
                any<ContainerOrchestratorClusterContext>(ContainerOrchestratorClusterContext::class.java), anyInt(), anyInt())).thenReturn(PollingResult.TIMEOUT)
        doNothing().`when`<ClusterBootstrapperErrorHandler>(clusterBootstrapperErrorHandler).terminateFailedNodes(any<HostOrchestrator>(HostOrchestrator::class.java), any<ContainerOrchestrator>(ContainerOrchestrator::class.java), any<Stack>(Stack::class.java),
                any<GatewayConfig>(GatewayConfig::class.java), any<Set<Any>>(Set<Any>::class.java))

        underTest!!.bootstrapNewNodes(stack.id, getPrivateIps(stack))
    }

    @Test(expected = CloudbreakException::class)
    @Throws(CloudbreakException::class, CloudbreakOrchestratorFailedException::class)
    fun bootstrapNewNodesInClusterWhenOrchestratorDropFailedException() {
        val stack = TestUtil.stack()

        `when`(stackRepository!!.findOneWithLists(anyLong())).thenReturn(stack)
        `when`(tlsSecurityService!!.buildGatewayConfig(anyLong(), anyString(), anyInt(), anyString(), anyString())).thenReturn(GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"))
        `when`(containerOrchestratorResolver!!.get("SWARM")).thenReturn(FailedNewNodesMockContainerOrchestrator())
        `when`(containerBootstrapApiPollingService!!.pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java), any<ContainerBootstrapApiContext>(ContainerBootstrapApiContext::class.java), anyInt(),
                anyInt())).thenReturn(PollingResult.SUCCESS)
        `when`(containerClusterAvailabilityPollingService!!.pollWithTimeoutSingleFailure(any<StatusCheckerTask<Any>>(StatusCheckerTask<Any>::class.java),
                any<ContainerOrchestratorClusterContext>(ContainerOrchestratorClusterContext::class.java), anyInt(), anyInt())).thenReturn(PollingResult.TIMEOUT)
        doNothing().`when`<ClusterBootstrapperErrorHandler>(clusterBootstrapperErrorHandler).terminateFailedNodes(any<HostOrchestrator>(HostOrchestrator::class.java), any<ContainerOrchestrator>(ContainerOrchestrator::class.java), any<Stack>(Stack::class.java),
                any<GatewayConfig>(GatewayConfig::class.java), any<Set<Any>>(Set<Any>::class.java))

        underTest!!.bootstrapNewNodes(stack.id, getPrivateIps(stack))
    }

    private fun getPrivateIps(stack: Stack): Set<String> {
        val ips = HashSet<String>()
        for (instanceMetaData in stack.runningInstanceMetaData) {
            ips.add(instanceMetaData.privateIp)
        }
        return ips
    }

    internal inner class FailedNewNodesMockContainerOrchestrator : MockContainerOrchestrator() {
        @Throws(CloudbreakOrchestratorFailedException::class)
        override fun bootstrapNewNodes(gatewayConfig: GatewayConfig, containerConfig: ContainerConfig, nodes: Set<Node>, exitCriteriaModel: ExitCriteriaModel) {
            throw CloudbreakOrchestratorFailedException("failed")
        }
    }

    internal inner class CancelledMockContainerOrchestrator : MockContainerOrchestrator() {
        @Throws(CloudbreakOrchestratorCancelledException::class)
        override fun bootstrap(gatewayConfig: GatewayConfig, config: ContainerConfig, nodes: Set<Node>, consulServerCount: Int, exitCriteriaModel: ExitCriteriaModel) {
            throw CloudbreakOrchestratorCancelledException("cancelled")
        }
    }

    internal inner class CancelledNewNodesMockContainerOrchestrator : MockContainerOrchestrator() {
        @Throws(CloudbreakOrchestratorCancelledException::class)
        override fun bootstrapNewNodes(gatewayConfig: GatewayConfig, containerConfig: ContainerConfig, nodes: Set<Node>, exitCriteriaModel: ExitCriteriaModel) {
            throw CloudbreakOrchestratorCancelledException("cancelled")
        }
    }

    internal inner class TwoLengthMockContainerOrchestrator : MockContainerOrchestrator() {
        override fun getMaxBootstrapNodes(): Int {
            return 2
        }
    }

    internal inner class FailedMockContainerOrchestrator : MockContainerOrchestrator() {
        @Throws(CloudbreakOrchestratorFailedException::class)
        override fun bootstrap(gatewayConfig: GatewayConfig, config: ContainerConfig, nodes: Set<Node>, consulServerCount: Int, exitCriteriaModel: ExitCriteriaModel) {
            throw CloudbreakOrchestratorFailedException("failed")
        }
    }

    companion object {

        private val GCP_PLATFORM = Platform.platform(CloudConstants.GCP)
    }
}
