package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.FlowCancelledException;
import com.sequenceiq.cloudbreak.core.flow.context.BootstrapApiContext;
import com.sequenceiq.cloudbreak.core.flow.context.ContainerOrchestratorClusterContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackScalingContext;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ScalingType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.orchestrator.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.Node;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;

@RunWith(MockitoJUnitRunner.class)
public class ClusterBootstrapperTest {

    @Mock
    private StackRepository stackRepository;

    @Mock
    private PollingService<BootstrapApiContext> bootstrapApiPollingService;

    @Mock
    private BootstrapApiCheckerTask bootstrapApiCheckerTask;

    @Mock
    private PollingService<ContainerOrchestratorClusterContext> clusterAvailabilityPollingService;

    @Mock
    private ClusterAvailabilityCheckerTask clusterAvailabilityCheckerTask;

    @Mock
    private ClusterBootstrapperErrorHandler clusterBootstrapperErrorHandler;

    @Mock
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @InjectMocks
    private ClusterBootstrapper underTest;

    @Test
    public void bootstrapClusterWhenEverythingWorksNormally() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();
        ProvisioningContext context = new ProvisioningContext.Builder().setAmbariIp("10.0.0.1").setDefaultParams(1L, CloudPlatform.AZURE).build();

        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString())).thenReturn(new GatewayConfig("10.0.0.1", "/cert/1"));
        when(containerOrchestratorResolver.get()).thenReturn(new MockContainerOrchestrator());
        when(bootstrapApiPollingService.pollWithTimeout(any(StatusCheckerTask.class), any(BootstrapApiContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        when(clusterAvailabilityPollingService.pollWithTimeout(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        doNothing().when(clusterBootstrapperErrorHandler)
                .terminateFailedNodes(any(ContainerOrchestrator.class), any(Stack.class), any(GatewayConfig.class), any(Set.class));

        underTest.bootstrapCluster(context);

        verify(tlsSecurityService, times(1)).buildGatewayConfig(anyLong(), anyString());
        verify(clusterBootstrapperErrorHandler, times(0))
                .terminateFailedNodes(any(ContainerOrchestrator.class), any(Stack.class), any(GatewayConfig.class), anySet());
        verify(bootstrapApiPollingService, times(1)).pollWithTimeout(any(StatusCheckerTask.class), any(BootstrapApiContext.class), anyInt(), anyInt());
        verify(clusterAvailabilityPollingService, times(1)).pollWithTimeout(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt());
    }

    @Test
    public void bootstrapClusterWhenTimeOutComesInClusterAvailabilityPoller() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();
        ProvisioningContext context = new ProvisioningContext.Builder().setAmbariIp("10.0.0.1").setDefaultParams(1L, CloudPlatform.AZURE).build();

        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString())).thenReturn(new GatewayConfig("10.0.0.1", "/cert/1"));
        when(containerOrchestratorResolver.get()).thenReturn(new MockContainerOrchestrator());
        when(bootstrapApiPollingService.pollWithTimeout(any(StatusCheckerTask.class), any(BootstrapApiContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        when(clusterAvailabilityPollingService.pollWithTimeout(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.TIMEOUT);
        doNothing().when(clusterBootstrapperErrorHandler)
                .terminateFailedNodes(any(ContainerOrchestrator.class), any(Stack.class), any(GatewayConfig.class), any(Set.class));

        underTest.bootstrapCluster(context);

        verify(tlsSecurityService, times(1)).buildGatewayConfig(anyLong(), anyString());
        verify(clusterBootstrapperErrorHandler, times(1))
                .terminateFailedNodes(any(ContainerOrchestrator.class), any(Stack.class), any(GatewayConfig.class), anySet());
        verify(bootstrapApiPollingService, times(1)).pollWithTimeout(any(StatusCheckerTask.class), any(BootstrapApiContext.class), anyInt(), anyInt());
        verify(clusterAvailabilityPollingService, times(1)).pollWithTimeout(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt());
    }

    @Test(expected = FlowCancelledException.class)
    public void bootstrapClusterWhenOrchestratorDropCancelledException() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();
        ProvisioningContext context = new ProvisioningContext.Builder().setAmbariIp("10.0.0.1").setDefaultParams(1L, CloudPlatform.AZURE).build();

        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString())).thenReturn(new GatewayConfig("10.0.0.1", "/cert/1"));
        when(containerOrchestratorResolver.get()).thenReturn(new CancelledMockContainerOrchestrator());
        when(bootstrapApiPollingService.pollWithTimeout(any(StatusCheckerTask.class), any(BootstrapApiContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        when(clusterAvailabilityPollingService.pollWithTimeout(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        doNothing().when(clusterBootstrapperErrorHandler)
                .terminateFailedNodes(any(ContainerOrchestrator.class), any(Stack.class), any(GatewayConfig.class), any(Set.class));

        underTest.bootstrapCluster(context);
    }

    @Test(expected = CloudbreakException.class)
    public void bootstrapClusterWhenOrchestratorDropFailedException() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();
        ProvisioningContext context = new ProvisioningContext.Builder().setAmbariIp("10.0.0.1").setDefaultParams(1L, CloudPlatform.AZURE).build();

        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString())).thenReturn(new GatewayConfig("10.0.0.1", "/cert/1"));
        when(containerOrchestratorResolver.get()).thenReturn(new FailedMockContainerOrchestrator());
        when(bootstrapApiPollingService.pollWithTimeout(any(StatusCheckerTask.class), any(BootstrapApiContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        when(clusterAvailabilityPollingService.pollWithTimeout(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        doNothing().when(clusterBootstrapperErrorHandler)
                .terminateFailedNodes(any(ContainerOrchestrator.class), any(Stack.class), any(GatewayConfig.class), any(Set.class));

        underTest.bootstrapCluster(context);
    }

    @Test
    public void bootstrapClusterWhenEverythingWorksNormallyWithMoreBootstrapSegment() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();
        ProvisioningContext context = new ProvisioningContext.Builder().setAmbariIp("10.0.0.1").setDefaultParams(1L, CloudPlatform.AZURE).build();

        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString())).thenReturn(new GatewayConfig("10.0.0.1", "/cert/1"));
        when(containerOrchestratorResolver.get()).thenReturn(new TwoLengthMockContainerOrchestrator());
        when(bootstrapApiPollingService.pollWithTimeout(any(StatusCheckerTask.class), any(BootstrapApiContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        when(clusterAvailabilityPollingService.pollWithTimeout(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt())).thenReturn(PollingResult.SUCCESS);
        doNothing().when(clusterBootstrapperErrorHandler)
                .terminateFailedNodes(any(ContainerOrchestrator.class), any(Stack.class), any(GatewayConfig.class), any(Set.class));

        underTest.bootstrapCluster(context);

        verify(clusterBootstrapperErrorHandler, times(0))
                .terminateFailedNodes(any(ContainerOrchestrator.class), any(Stack.class), any(GatewayConfig.class), anySet());
        verify(tlsSecurityService, times(1)).buildGatewayConfig(anyLong(), anyString());
        verify(bootstrapApiPollingService, times(1)).pollWithTimeout(any(StatusCheckerTask.class), any(BootstrapApiContext.class), anyInt(), anyInt());
        verify(clusterAvailabilityPollingService, times(3)).pollWithTimeout(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt());
    }

    @Test
    public void bootstrapNewNodesInClusterWhenEverythingWorksNormally() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();
        StackScalingContext context = new StackScalingContext(1L, CloudPlatform.AZURE, 2, "master1", new HashSet<Resource>(),
                ScalingType.UPSCALE_ONLY_STACK, getPrivateIps(stack));

        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString())).thenReturn(new GatewayConfig("10.0.0.1", "/cert/1"));
        when(containerOrchestratorResolver.get()).thenReturn(new MockContainerOrchestrator());
        when(bootstrapApiPollingService.pollWithTimeout(any(StatusCheckerTask.class), any(BootstrapApiContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        when(clusterAvailabilityPollingService.pollWithTimeout(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        doNothing().when(clusterBootstrapperErrorHandler)
                .terminateFailedNodes(any(ContainerOrchestrator.class), any(Stack.class), any(GatewayConfig.class), any(Set.class));

        underTest.bootstrapNewNodes(context);

        verify(clusterBootstrapperErrorHandler, times(0))
                .terminateFailedNodes(any(ContainerOrchestrator.class), any(Stack.class), any(GatewayConfig.class), anySet());
        verify(tlsSecurityService, times(1)).buildGatewayConfig(anyLong(), anyString());
        verify(clusterAvailabilityPollingService, times(2)).pollWithTimeout(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt());
    }

    @Test
    public void bootstrapNewNodesInClusterWhenBootstrapHappeningInTwoSegments() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();
        StackScalingContext context = new StackScalingContext(1L, CloudPlatform.AZURE, 2, "master1", new HashSet<Resource>(),
                ScalingType.UPSCALE_ONLY_STACK, getPrivateIps(stack));

        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString())).thenReturn(new GatewayConfig("10.0.0.1", "/cert/1"));
        when(containerOrchestratorResolver.get()).thenReturn(new TwoLengthMockContainerOrchestrator());
        when(bootstrapApiPollingService.pollWithTimeout(any(StatusCheckerTask.class), any(BootstrapApiContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        when(clusterAvailabilityPollingService.pollWithTimeout(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        doNothing().when(clusterBootstrapperErrorHandler)
                .terminateFailedNodes(any(ContainerOrchestrator.class), any(Stack.class), any(GatewayConfig.class), any(Set.class));

        underTest.bootstrapNewNodes(context);

        verify(clusterBootstrapperErrorHandler, times(0))
                .terminateFailedNodes(any(ContainerOrchestrator.class), any(Stack.class), any(GatewayConfig.class), anySet());
        verify(tlsSecurityService, times(1)).buildGatewayConfig(anyLong(), anyString());
        verify(clusterAvailabilityPollingService, times(3)).pollWithTimeout(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt());
    }

    @Test
    public void bootstrapNewNodesInClusterWhenClusterAvailabilityDropTimeout() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();
        StackScalingContext context = new StackScalingContext(1L, CloudPlatform.AZURE, 2, "master1", new HashSet<Resource>(),
                ScalingType.UPSCALE_ONLY_STACK, getPrivateIps(stack));

        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString())).thenReturn(new GatewayConfig("10.0.0.1", "/cert/1"));
        when(containerOrchestratorResolver.get()).thenReturn(new MockContainerOrchestrator());
        when(bootstrapApiPollingService.pollWithTimeout(any(StatusCheckerTask.class), any(BootstrapApiContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        when(clusterAvailabilityPollingService.pollWithTimeout(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.TIMEOUT);
        doNothing().when(clusterBootstrapperErrorHandler)
                .terminateFailedNodes(any(ContainerOrchestrator.class), any(Stack.class), any(GatewayConfig.class), any(Set.class));

        underTest.bootstrapNewNodes(context);

        verify(clusterBootstrapperErrorHandler, times(1))
                .terminateFailedNodes(any(ContainerOrchestrator.class), any(Stack.class), any(GatewayConfig.class), anySet());
        verify(tlsSecurityService, times(1)).buildGatewayConfig(anyLong(), anyString());
        verify(clusterAvailabilityPollingService, times(2)).pollWithTimeout(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt());
    }

    @Test(expected = FlowCancelledException.class)
    public void bootstrapNewNodesInClusterWhenOrchestratorDropCancelledException() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();
        StackScalingContext context = new StackScalingContext(1L, CloudPlatform.AZURE, 2, "master1", new HashSet<Resource>(),
                ScalingType.UPSCALE_ONLY_STACK, getPrivateIps(stack));

        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString())).thenReturn(new GatewayConfig("10.0.0.1", "/cert/1"));
        when(containerOrchestratorResolver.get()).thenReturn(new CancelledNewNodesMockContainerOrchestrator());
        when(bootstrapApiPollingService.pollWithTimeout(any(StatusCheckerTask.class), any(BootstrapApiContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        when(clusterAvailabilityPollingService.pollWithTimeout(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.TIMEOUT);
        doNothing().when(clusterBootstrapperErrorHandler)
                .terminateFailedNodes(any(ContainerOrchestrator.class), any(Stack.class), any(GatewayConfig.class), any(Set.class));

        underTest.bootstrapNewNodes(context);
    }

    @Test(expected = CloudbreakException.class)
    public void bootstrapNewNodesInClusterWhenOrchestratorDropFailedException() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();
        StackScalingContext context = new StackScalingContext(1L, CloudPlatform.AZURE, 2, "master1", new HashSet<Resource>(),
                ScalingType.UPSCALE_ONLY_STACK, getPrivateIps(stack));

        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString())).thenReturn(new GatewayConfig("10.0.0.1", "/cert/1"));
        when(containerOrchestratorResolver.get()).thenReturn(new FailedNewNodesMockContainerOrchestrator());
        when(bootstrapApiPollingService.pollWithTimeout(any(StatusCheckerTask.class), any(BootstrapApiContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        when(clusterAvailabilityPollingService.pollWithTimeout(any(StatusCheckerTask.class),
                any(ContainerOrchestratorClusterContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.TIMEOUT);
        doNothing().when(clusterBootstrapperErrorHandler)
                .terminateFailedNodes(any(ContainerOrchestrator.class), any(Stack.class), any(GatewayConfig.class), any(Set.class));

        underTest.bootstrapNewNodes(context);
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
        public void bootstrapNewNodes(GatewayConfig gatewayConfig, Set<Node> nodes, ExitCriteriaModel exitCriteriaModel)
                throws CloudbreakOrchestratorFailedException {
            throw new CloudbreakOrchestratorFailedException("failed");
        }
    }

    class CancelledMockContainerOrchestrator extends MockContainerOrchestrator {
        @Override
        public void bootstrap(GatewayConfig gatewayConfig, Set<Node> nodes, int consulServerCount, ExitCriteriaModel exitCriteriaModel)
                throws CloudbreakOrchestratorCancelledException {
            throw new CloudbreakOrchestratorCancelledException("cancelled");
        }
    }

    class CancelledNewNodesMockContainerOrchestrator extends MockContainerOrchestrator {
        @Override
        public void bootstrapNewNodes(GatewayConfig gatewayConfig, Set<Node> nodes, ExitCriteriaModel exitCriteriaModel)
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
        public void bootstrap(GatewayConfig gatewayConfig, Set<Node> nodes, int consulServerCount, ExitCriteriaModel exitCriteriaModel)
                throws CloudbreakOrchestratorFailedException {
            throw new CloudbreakOrchestratorFailedException("failed");
        }
    }
}