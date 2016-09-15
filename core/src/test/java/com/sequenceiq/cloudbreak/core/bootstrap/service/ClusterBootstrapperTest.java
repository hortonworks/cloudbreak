package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerBootstrapApiCheckerTask;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerClusterAvailabilityCheckerTask;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.context.ContainerBootstrapApiContext;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.context.ContainerOrchestratorClusterContext;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostBootstrapApiCheckerTask;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostClusterAvailabilityCheckerTask;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.context.HostBootstrapApiContext;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.context.HostOrchestratorClusterContext;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.repository.OrchestratorRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;

@RunWith(MockitoJUnitRunner.class)
public class ClusterBootstrapperTest {

    private static final com.sequenceiq.cloudbreak.cloud.model.Platform GCP_PLATFORM = Platform.platform(CloudConstants.GCP);

    @Mock
    private StackRepository stackRepository;

    @Mock
    private OrchestratorRepository orchestratorRepository;

    @Mock
    private PollingService<ContainerBootstrapApiContext> containerBootstrapApiPollingService;

    @Mock
    private PollingService<HostBootstrapApiContext> hostBootstrapApiPollingService;

    @Mock
    private ContainerBootstrapApiCheckerTask containerBootstrapApiCheckerTask;

    @Mock
    private HostBootstrapApiCheckerTask hostBootstrapApiCheckerTask;

    @Mock
    private PollingService<ContainerOrchestratorClusterContext> containerClusterAvailabilityPollingService;

    @Mock
    private PollingService<HostOrchestratorClusterContext> hostClusterAvailabilityPollingService;

    @Mock
    private ContainerClusterAvailabilityCheckerTask containerClusterAvailabilityCheckerTask;

    @Mock
    private HostClusterAvailabilityCheckerTask hostClusterAvailabilityCheckerTask;

    @Mock
    private ClusterBootstrapperErrorHandler clusterBootstrapperErrorHandler;

    @Mock
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    @Mock
    private HostOrchestratorResolver hostOrchestratorResolver;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private ContainerConfigService containerConfigService;

    @Mock
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @InjectMocks
    private ClusterBootstrapper underTest;

    @Before
    public void setUp() throws CloudbreakException {
        reset(stackRepository, orchestratorRepository, containerBootstrapApiPollingService, hostBootstrapApiPollingService, gatewayConfigService,
                containerBootstrapApiCheckerTask, containerOrchestratorResolver, containerClusterAvailabilityPollingService,
                hostClusterAvailabilityPollingService, containerClusterAvailabilityCheckerTask, clusterBootstrapperErrorHandler, hostOrchestratorResolver,
                containerConfigService, orchestratorTypeResolver);
        when(orchestratorTypeResolver.resolveType(anyString())).thenReturn(OrchestratorType.CONTAINER);
        ReflectionTestUtils.setField(containerConfigService, "munchausenImageName", "sequence/testcont:0.1.1");
    }

    @Test
    public void bootstrapClusterWhenEverythingWorksNormally() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();

        when(gatewayConfigService.getGatewayConfig(any()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"));
        when(containerOrchestratorResolver.get("SWARM")).thenReturn(new MockContainerOrchestrator());
        when(containerBootstrapApiPollingService.pollWithTimeoutSingleFailure(any(StatusCheckerTask.class), any(ContainerBootstrapApiContext.class), anyInt(),
                anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        when(containerClusterAvailabilityPollingService.pollWithTimeoutSingleFailure(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        doNothing().when(clusterBootstrapperErrorHandler)
                .terminateFailedNodes(any(HostOrchestrator.class), any(ContainerOrchestrator.class), any(Stack.class),
                        any(GatewayConfig.class), any(Set.class));
        when(orchestratorRepository.save(any(Orchestrator.class))).thenReturn(new Orchestrator());
        underTest.bootstrapContainers(stack);

        verify(gatewayConfigService, times(1)).getGatewayConfig(any(), any());
        verify(clusterBootstrapperErrorHandler, times(0))
                .terminateFailedNodes(any(HostOrchestrator.class), any(ContainerOrchestrator.class), any(Stack.class), any(GatewayConfig.class), anySet());
        verify(containerBootstrapApiPollingService, times(1)).pollWithTimeoutSingleFailure(any(StatusCheckerTask.class),
                any(ContainerBootstrapApiContext.class), anyInt(),
                anyInt());
        verify(containerClusterAvailabilityPollingService, times(1)).pollWithTimeoutSingleFailure(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt());
    }

    @Test
    public void bootstrapClusterWhenTimeOutComesInClusterAvailabilityPoller() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();
        when(gatewayConfigService.getGatewayConfig(any(), any()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"));
        when(containerOrchestratorResolver.get("SWARM")).thenReturn(new MockContainerOrchestrator());
        when(containerBootstrapApiPollingService.pollWithTimeoutSingleFailure(any(StatusCheckerTask.class), any(ContainerBootstrapApiContext.class), anyInt(),
                anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        when(containerClusterAvailabilityPollingService.pollWithTimeoutSingleFailure(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.TIMEOUT);
        doNothing().when(clusterBootstrapperErrorHandler)
                .terminateFailedNodes(any(HostOrchestrator.class), any(ContainerOrchestrator.class), any(Stack.class),
                        any(GatewayConfig.class), any(Set.class));

        underTest.bootstrapContainers(stack);

        verify(gatewayConfigService, times(1)).getGatewayConfig(any(), any());
        verify(clusterBootstrapperErrorHandler, times(1))
                .terminateFailedNodes(any(HostOrchestrator.class), any(ContainerOrchestrator.class), any(Stack.class), any(GatewayConfig.class), anySet());
        verify(containerBootstrapApiPollingService, times(1)).pollWithTimeoutSingleFailure(any(StatusCheckerTask.class),
                any(ContainerBootstrapApiContext.class), anyInt(),
                anyInt());
        verify(containerClusterAvailabilityPollingService, times(1)).pollWithTimeoutSingleFailure(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt());
    }

    @Test(expected = CancellationException.class)
    public void bootstrapClusterWhenOrchestratorDropCancelledException() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();

        when(gatewayConfigService.getGatewayConfig(any(), any()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"));
        when(containerOrchestratorResolver.get("SWARM")).thenReturn(new CancelledMockContainerOrchestrator());
        when(containerBootstrapApiPollingService.pollWithTimeoutSingleFailure(any(StatusCheckerTask.class), any(ContainerBootstrapApiContext.class), anyInt(),
                anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        when(containerClusterAvailabilityPollingService.pollWithTimeoutSingleFailure(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        doNothing().when(clusterBootstrapperErrorHandler)
                .terminateFailedNodes(any(HostOrchestrator.class), any(ContainerOrchestrator.class), any(Stack.class),
                        any(GatewayConfig.class), any(Set.class));

        underTest.bootstrapContainers(stack);
    }

    @Test(expected = CloudbreakException.class)
    public void bootstrapClusterWhenOrchestratorDropFailedException() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();

        when(gatewayConfigService.getGatewayConfig(any(), any()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"));
        when(containerOrchestratorResolver.get("SWARM")).thenReturn(new FailedMockContainerOrchestrator());
        when(containerBootstrapApiPollingService.pollWithTimeoutSingleFailure(any(StatusCheckerTask.class), any(ContainerBootstrapApiContext.class), anyInt(),
                anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        when(containerClusterAvailabilityPollingService.pollWithTimeoutSingleFailure(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        doNothing().when(clusterBootstrapperErrorHandler)
                .terminateFailedNodes(any(HostOrchestrator.class), any(ContainerOrchestrator.class), any(Stack.class),
                        any(GatewayConfig.class), any(Set.class));

        underTest.bootstrapContainers(stack);
    }

    @Test
    public void bootstrapClusterWhenEverythingWorksNormallyWithMoreBootstrapSegment() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();

        when(gatewayConfigService.getGatewayConfig(any(), any()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"));
        when(containerOrchestratorResolver.get("SWARM")).thenReturn(new TwoLengthMockContainerOrchestrator());
        when(containerBootstrapApiPollingService.pollWithTimeoutSingleFailure(any(StatusCheckerTask.class), any(ContainerBootstrapApiContext.class), anyInt(),
                anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        when(containerClusterAvailabilityPollingService.pollWithTimeoutSingleFailure(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt())).thenReturn(PollingResult.SUCCESS);
        doNothing().when(clusterBootstrapperErrorHandler)
                .terminateFailedNodes(any(HostOrchestrator.class), any(ContainerOrchestrator.class), any(Stack.class),
                        any(GatewayConfig.class), any(Set.class));

        underTest.bootstrapContainers(stack);

        verify(clusterBootstrapperErrorHandler, times(0))
                .terminateFailedNodes(any(HostOrchestrator.class), any(ContainerOrchestrator.class), any(Stack.class), any(GatewayConfig.class), anySet());
        verify(gatewayConfigService, times(1)).getGatewayConfig(any(), any());
        verify(containerBootstrapApiPollingService, times(1)).pollWithTimeoutSingleFailure(any(StatusCheckerTask.class),
                any(ContainerBootstrapApiContext.class), anyInt(), anyInt());
        verify(containerClusterAvailabilityPollingService, times(3)).pollWithTimeoutSingleFailure(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt());
    }

    @Test
    public void bootstrapNewNodesInClusterWhenEverythingWorksNormally() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();

        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(gatewayConfigService.getGatewayConfig(any(), any()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"));
        when(containerOrchestratorResolver.get("SWARM")).thenReturn(new MockContainerOrchestrator());
        when(containerBootstrapApiPollingService.pollWithTimeoutSingleFailure(any(StatusCheckerTask.class), any(ContainerBootstrapApiContext.class), anyInt(),
                anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        when(containerClusterAvailabilityPollingService.pollWithTimeoutSingleFailure(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        doNothing().when(clusterBootstrapperErrorHandler)
                .terminateFailedNodes(any(HostOrchestrator.class), any(ContainerOrchestrator.class), any(Stack.class),
                        any(GatewayConfig.class), any(Set.class));

        underTest.bootstrapNewNodes(stack.getId(), getPrivateIps(stack));

        verify(clusterBootstrapperErrorHandler, times(0))
                .terminateFailedNodes(any(HostOrchestrator.class), any(ContainerOrchestrator.class), any(Stack.class), any(GatewayConfig.class), anySet());
        verify(gatewayConfigService, times(1)).getGatewayConfig(any(), any());
        verify(containerClusterAvailabilityPollingService, times(2)).pollWithTimeoutSingleFailure(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt());
    }

    @Test
    public void bootstrapNewNodesInClusterWhenBootstrapHappeningInTwoSegments() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();

        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(gatewayConfigService.getGatewayConfig(any(), any()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"));
        when(containerOrchestratorResolver.get("SWARM")).thenReturn(new TwoLengthMockContainerOrchestrator());
        when(containerBootstrapApiPollingService.pollWithTimeoutSingleFailure(any(StatusCheckerTask.class), any(ContainerBootstrapApiContext.class), anyInt(),
                anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        when(containerClusterAvailabilityPollingService.pollWithTimeoutSingleFailure(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        doNothing().when(clusterBootstrapperErrorHandler)
                .terminateFailedNodes(any(HostOrchestrator.class), any(ContainerOrchestrator.class), any(Stack.class),
                        any(GatewayConfig.class), any(Set.class));

        underTest.bootstrapNewNodes(stack.getId(), getPrivateIps(stack));
        verify(clusterBootstrapperErrorHandler, times(0))
                .terminateFailedNodes(any(HostOrchestrator.class), any(ContainerOrchestrator.class), any(Stack.class), any(GatewayConfig.class), anySet());
        verify(gatewayConfigService, times(1)).getGatewayConfig(any(), any());
        verify(containerClusterAvailabilityPollingService, times(3)).pollWithTimeoutSingleFailure(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt());
    }

    @Test
    public void bootstrapNewNodesInClusterWhenClusterAvailabilityDropTimeout() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();

        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(gatewayConfigService.getGatewayConfig(any()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"));
        when(containerOrchestratorResolver.get("SWARM")).thenReturn(new MockContainerOrchestrator());
        when(containerBootstrapApiPollingService.pollWithTimeoutSingleFailure(any(StatusCheckerTask.class), any(ContainerBootstrapApiContext.class), anyInt(),
                anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        when(containerClusterAvailabilityPollingService.pollWithTimeoutSingleFailure(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.TIMEOUT);
        doNothing().when(clusterBootstrapperErrorHandler)
                .terminateFailedNodes(any(HostOrchestrator.class), any(ContainerOrchestrator.class), any(Stack.class),
                        any(GatewayConfig.class), any(Set.class));

        underTest.bootstrapNewNodes(stack.getId(), getPrivateIps(stack));

        verify(clusterBootstrapperErrorHandler, times(1))
                .terminateFailedNodes(any(HostOrchestrator.class), any(ContainerOrchestrator.class), any(Stack.class), any(GatewayConfig.class), anySet());
        verify(gatewayConfigService, times(1)).getGatewayConfig(any(), any());
        verify(containerClusterAvailabilityPollingService, times(2)).pollWithTimeoutSingleFailure(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt());
    }

    @Test(expected = CancellationException.class)
    public void bootstrapNewNodesInClusterWhenOrchestratorDropCancelledException() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();

        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(gatewayConfigService.getGatewayConfig(any(), any()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"));
        when(containerOrchestratorResolver.get("SWARM")).thenReturn(new CancelledNewNodesMockContainerOrchestrator());
        when(containerBootstrapApiPollingService.pollWithTimeoutSingleFailure(any(StatusCheckerTask.class), any(ContainerBootstrapApiContext.class), anyInt(),
                anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        when(containerClusterAvailabilityPollingService.pollWithTimeoutSingleFailure(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.TIMEOUT);
        doNothing().when(clusterBootstrapperErrorHandler)
                .terminateFailedNodes(any(HostOrchestrator.class), any(ContainerOrchestrator.class), any(Stack.class),
                        any(GatewayConfig.class), any(Set.class));

        underTest.bootstrapNewNodes(stack.getId(), getPrivateIps(stack));
    }

    @Test(expected = CloudbreakException.class)
    public void bootstrapNewNodesInClusterWhenOrchestratorDropFailedException() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();

        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(gatewayConfigService.getGatewayConfig(any(), any()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"));
        when(containerOrchestratorResolver.get("SWARM")).thenReturn(new FailedNewNodesMockContainerOrchestrator());
        when(containerBootstrapApiPollingService.pollWithTimeoutSingleFailure(any(StatusCheckerTask.class), any(ContainerBootstrapApiContext.class), anyInt(),
                anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        when(containerClusterAvailabilityPollingService.pollWithTimeoutSingleFailure(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.TIMEOUT);
        doNothing().when(clusterBootstrapperErrorHandler)
                .terminateFailedNodes(any(HostOrchestrator.class), any(ContainerOrchestrator.class), any(Stack.class),
                        any(GatewayConfig.class), any(Set.class));

        underTest.bootstrapNewNodes(stack.getId(), getPrivateIps(stack));
    }

    private Set<String> getPrivateIps(Stack stack) {
        Set<String> ips = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            ips.add(instanceMetaData.getPrivateIp());
        }
        return ips;
    }

    class FailedNewNodesMockContainerOrchestrator extends MockContainerOrchestrator {
        @Override
        public void bootstrapNewNodes(GatewayConfig gatewayConfig, ContainerConfig containerConfig, Set<Node> nodes, ExitCriteriaModel
                exitCriteriaModel)
                throws CloudbreakOrchestratorFailedException {
            throw new CloudbreakOrchestratorFailedException("failed");
        }
    }

    class CancelledMockContainerOrchestrator extends MockContainerOrchestrator {
        @Override
        public void bootstrap(GatewayConfig gatewayConfig, ContainerConfig config, Set<Node> nodes, int consulServerCount, ExitCriteriaModel exitCriteriaModel)
                throws CloudbreakOrchestratorCancelledException {
            throw new CloudbreakOrchestratorCancelledException("cancelled");
        }
    }

    class CancelledNewNodesMockContainerOrchestrator extends MockContainerOrchestrator {
        @Override
        public void bootstrapNewNodes(GatewayConfig gatewayConfig, ContainerConfig containerConfig, Set<Node> nodes, ExitCriteriaModel exitCriteriaModel)
                throws CloudbreakOrchestratorCancelledException {
            throw new CloudbreakOrchestratorCancelledException("cancelled");
        }
    }

    class TwoLengthMockContainerOrchestrator extends MockContainerOrchestrator {
        @Override
        public int getMaxBootstrapNodes() {
            return 2;
        }
    }

    class FailedMockContainerOrchestrator extends MockContainerOrchestrator {
        @Override
        public void bootstrap(GatewayConfig gatewayConfig, ContainerConfig config, Set<Node> nodes, int consulServerCount, ExitCriteriaModel exitCriteriaModel)
                throws CloudbreakOrchestratorFailedException {
            throw new CloudbreakOrchestratorFailedException("failed");
        }
    }
}
