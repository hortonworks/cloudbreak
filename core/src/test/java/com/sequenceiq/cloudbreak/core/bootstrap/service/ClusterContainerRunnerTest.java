package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.FlowCancelledException;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.ScalingType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestratorCluster;
import com.sequenceiq.cloudbreak.orchestrator.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.orchestrator.GatewayConfig;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;

@RunWith(MockitoJUnitRunner.class)
public class ClusterContainerRunnerTest {

    @Mock
    private StackRepository stackRepository;

    @Mock
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private MockContainerOrchestrator mockContainerOrchestrator;

    @Mock
    private CancelledMockContainerOrchestrator cancelledMockContainerOrchestrator;

    @Mock
    private FailedMockContainerOrchestrator failedMockContainerOrchestrator;

    @InjectMocks
    private ClusterContainerRunner underTest;

    @Test
    public void runClusterContainersWhenBaywatchEnabled() throws CloudbreakException, CloudbreakOrchestratorFailedException,
            CloudbreakOrchestratorCancelledException {
        ReflectionTestUtils.setField(underTest, "baywatchEnabled", true);
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster(TestUtil.blueprint(), stack, 1L));
        ProvisioningContext provisioningContext = new ProvisioningContext.Builder().setDefaultParams(1L, CloudPlatform.AZURE).build();

        when(containerOrchestratorResolver.get()).thenReturn(mockContainerOrchestrator);
        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString())).thenReturn(new GatewayConfig("10.0.0.1", "/cert/1"));

        underTest.runClusterContainers(provisioningContext);

        verify(mockContainerOrchestrator, times(1)).startAmbariAgents(any(ContainerOrchestratorCluster.class), anyString(), anyInt(),
                anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startAmbariServer(any(ContainerOrchestratorCluster.class), anyString(), anyString(),
                anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startBaywatchClients(any(ContainerOrchestratorCluster.class), anyString(), anyString(),
                anyInt(), anyString(), anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startBaywatchServer(any(ContainerOrchestratorCluster.class), anyString(),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startConsulWatches(any(ContainerOrchestratorCluster.class), anyString(), anyInt(),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startRegistrator(any(ContainerOrchestratorCluster.class), anyString(), any(ExitCriteriaModel.class));
    }

    @Test(expected = CloudbreakException.class)
    public void runClusterContainersWhenContainerRunnerFailed()
            throws CloudbreakException, CloudbreakOrchestratorFailedException, CloudbreakOrchestratorCancelledException {
        ReflectionTestUtils.setField(underTest, "baywatchEnabled", true);
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster(TestUtil.blueprint(), stack, 1L));
        ProvisioningContext provisioningContext = new ProvisioningContext.Builder().setDefaultParams(1L, CloudPlatform.AZURE).build();

        when(containerOrchestratorResolver.get()).thenReturn(new FailedMockContainerOrchestrator());
        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString())).thenReturn(new GatewayConfig("10.0.0.1", "/cert/1"));

        underTest.runClusterContainers(provisioningContext);
    }

    @Test(expected = FlowCancelledException.class)
    public void runClusterContainersWhenContainerRunnerCancelled()
            throws CloudbreakException, CloudbreakOrchestratorFailedException, CloudbreakOrchestratorCancelledException {
        ReflectionTestUtils.setField(underTest, "baywatchEnabled", true);
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster(TestUtil.blueprint(), stack, 1L));
        ProvisioningContext provisioningContext = new ProvisioningContext.Builder().setDefaultParams(1L, CloudPlatform.AZURE).build();

        when(containerOrchestratorResolver.get()).thenReturn(new CancelledMockContainerOrchestrator());
        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString())).thenReturn(new GatewayConfig("10.0.0.1", "/cert/1"));

        underTest.runClusterContainers(provisioningContext);
    }

    @Test
    public void runClusterContainersWhenBaywatchEnabledAndBaywatchServerExternLocationNotNull()
            throws CloudbreakException, CloudbreakOrchestratorFailedException, CloudbreakOrchestratorCancelledException {
        ReflectionTestUtils.setField(underTest, "baywatchEnabled", true);
        ReflectionTestUtils.setField(underTest, "baywatchServerExternLocation", "test");
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster(TestUtil.blueprint(), stack, 1L));
        ProvisioningContext provisioningContext = new ProvisioningContext.Builder().setDefaultParams(1L, CloudPlatform.AZURE).build();

        when(containerOrchestratorResolver.get()).thenReturn(mockContainerOrchestrator);
        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString())).thenReturn(new GatewayConfig("10.0.0.1", "/cert/1"));

        underTest.runClusterContainers(provisioningContext);

        verify(mockContainerOrchestrator, times(1)).startAmbariAgents(any(ContainerOrchestratorCluster.class), anyString(), anyInt(),
                anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startAmbariServer(any(ContainerOrchestratorCluster.class), anyString(), anyString(),
                anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startBaywatchClients(any(ContainerOrchestratorCluster.class), anyString(), anyString(),
                anyInt(), anyString(), anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startBaywatchServer(any(ContainerOrchestratorCluster.class), anyString(),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startConsulWatches(any(ContainerOrchestratorCluster.class), anyString(), anyInt(),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startRegistrator(any(ContainerOrchestratorCluster.class), anyString(), any(ExitCriteriaModel.class));
    }

    @Test
    public void runClusterContainersWhenBaywatchDisabled() throws CloudbreakException, CloudbreakOrchestratorFailedException,
            CloudbreakOrchestratorCancelledException {
        ReflectionTestUtils.setField(underTest, "baywatchEnabled", false);
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster(TestUtil.blueprint(), stack, 1L));
        ProvisioningContext provisioningContext = new ProvisioningContext.Builder().setDefaultParams(1L, CloudPlatform.AZURE).build();

        when(containerOrchestratorResolver.get()).thenReturn(mockContainerOrchestrator);
        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString())).thenReturn(new GatewayConfig("10.0.0.1", "/cert/1"));

        underTest.runClusterContainers(provisioningContext);

        verify(mockContainerOrchestrator, times(1)).startAmbariAgents(any(ContainerOrchestratorCluster.class), anyString(), anyInt(),
                anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startAmbariServer(any(ContainerOrchestratorCluster.class), anyString(), anyString(),
                anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startBaywatchClients(any(ContainerOrchestratorCluster.class), anyString(), anyString(),
                anyInt(), anyString(), anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startBaywatchServer(any(ContainerOrchestratorCluster.class), anyString(),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startConsulWatches(any(ContainerOrchestratorCluster.class), anyString(), anyInt(),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startRegistrator(any(ContainerOrchestratorCluster.class), anyString(), any(ExitCriteriaModel.class));
    }

    @Test
    public void runNewNodesClusterContainersWhenBaywatchEnabled() throws CloudbreakException, CloudbreakOrchestratorFailedException,
            CloudbreakOrchestratorCancelledException {
        ReflectionTestUtils.setField(underTest, "baywatchEnabled", true);
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster(TestUtil.blueprint(), stack, 1L));
        HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
        ClusterScalingContext context = new ClusterScalingContext(1L, CloudPlatform.AZURE, hostGroupAdjustmentJson, getPrivateIps(stack),
                new ArrayList<HostMetadata>(), ScalingType.UPSCALE_ONLY_CLUSTER);
        when(containerOrchestratorResolver.get()).thenReturn(mockContainerOrchestrator);
        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString())).thenReturn(new GatewayConfig("10.0.0.1", "/cert/1"));

        underTest.addClusterContainers(context);

        verify(mockContainerOrchestrator, times(1)).startAmbariAgents(any(ContainerOrchestratorCluster.class), anyString(), anyInt(),
                anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startAmbariServer(any(ContainerOrchestratorCluster.class), anyString(), anyString(),
                anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startBaywatchClients(any(ContainerOrchestratorCluster.class), anyString(), anyString(),
                anyInt(), anyString(), anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startBaywatchServer(any(ContainerOrchestratorCluster.class), anyString(),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startConsulWatches(any(ContainerOrchestratorCluster.class), anyString(), anyInt(),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startRegistrator(any(ContainerOrchestratorCluster.class), anyString(), any(ExitCriteriaModel.class));
    }

    @Test(expected = CloudbreakException.class)
    public void runNewNodesClusterContainersWhenContainerRunnerFailed()
            throws CloudbreakException, CloudbreakOrchestratorFailedException, CloudbreakOrchestratorCancelledException {
        ReflectionTestUtils.setField(underTest, "baywatchEnabled", true);
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster(TestUtil.blueprint(), stack, 1L));
        HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
        ClusterScalingContext context = new ClusterScalingContext(1L, CloudPlatform.AZURE, hostGroupAdjustmentJson, getPrivateIps(stack),
                new ArrayList<HostMetadata>(), ScalingType.UPSCALE_ONLY_CLUSTER);
        when(containerOrchestratorResolver.get()).thenReturn(new FailedMockContainerOrchestrator());
        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString())).thenReturn(new GatewayConfig("10.0.0.1", "/cert/1"));

        underTest.addClusterContainers(context);
    }

    @Test(expected = FlowCancelledException.class)
    public void runNewNodesClusterContainersWhenContainerRunnerCancelled()
            throws CloudbreakException, CloudbreakOrchestratorFailedException, CloudbreakOrchestratorCancelledException {
        ReflectionTestUtils.setField(underTest, "baywatchEnabled", true);
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster(TestUtil.blueprint(), stack, 1L));
        HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
        ClusterScalingContext context = new ClusterScalingContext(1L, CloudPlatform.AZURE, hostGroupAdjustmentJson, getPrivateIps(stack),
                new ArrayList<HostMetadata>(), ScalingType.UPSCALE_ONLY_CLUSTER);
        when(containerOrchestratorResolver.get()).thenReturn(new CancelledMockContainerOrchestrator());
        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString())).thenReturn(new GatewayConfig("10.0.0.1", "/cert/1"));

        underTest.addClusterContainers(context);
    }

    @Test
    public void runNewNodesClusterContainersWhenBaywatchEnabledAndBaywatchServerExternLocationNotNull()
            throws CloudbreakException, CloudbreakOrchestratorFailedException, CloudbreakOrchestratorCancelledException {
        ReflectionTestUtils.setField(underTest, "baywatchEnabled", true);
        ReflectionTestUtils.setField(underTest, "baywatchServerExternLocation", "test");
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster(TestUtil.blueprint(), stack, 1L));
        HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
        ClusterScalingContext context = new ClusterScalingContext(1L, CloudPlatform.AZURE, hostGroupAdjustmentJson, getPrivateIps(stack),
                new ArrayList<HostMetadata>(), ScalingType.UPSCALE_ONLY_CLUSTER);
        when(containerOrchestratorResolver.get()).thenReturn(mockContainerOrchestrator);
        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString())).thenReturn(new GatewayConfig("10.0.0.1", "/cert/1"));

        underTest.addClusterContainers(context);

        verify(mockContainerOrchestrator, times(1)).startAmbariAgents(any(ContainerOrchestratorCluster.class), anyString(), anyInt(),
                anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startAmbariServer(any(ContainerOrchestratorCluster.class), anyString(), anyString(),
                anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startBaywatchClients(any(ContainerOrchestratorCluster.class), anyString(), anyString(),
                anyInt(), anyString(), anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startBaywatchServer(any(ContainerOrchestratorCluster.class), anyString(),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startConsulWatches(any(ContainerOrchestratorCluster.class), anyString(), anyInt(),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startRegistrator(any(ContainerOrchestratorCluster.class), anyString(), any(ExitCriteriaModel.class));
    }

    @Test
    public void runNewNodesClusterContainersWhenBaywatchDisabled() throws CloudbreakException, CloudbreakOrchestratorFailedException,
            CloudbreakOrchestratorCancelledException {
        ReflectionTestUtils.setField(underTest, "baywatchEnabled", false);
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster(TestUtil.blueprint(), stack, 1L));
        HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
        ClusterScalingContext context = new ClusterScalingContext(1L, CloudPlatform.AZURE, hostGroupAdjustmentJson, getPrivateIps(stack),
                new ArrayList<HostMetadata>(), ScalingType.UPSCALE_ONLY_CLUSTER);

        when(containerOrchestratorResolver.get()).thenReturn(mockContainerOrchestrator);
        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString())).thenReturn(new GatewayConfig("10.0.0.1", "/cert/1"));

        underTest.addClusterContainers(context);

        verify(mockContainerOrchestrator, times(1)).startAmbariAgents(any(ContainerOrchestratorCluster.class), anyString(), anyInt(),
                anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startAmbariServer(any(ContainerOrchestratorCluster.class), anyString(), anyString(),
                anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startBaywatchClients(any(ContainerOrchestratorCluster.class), anyString(), anyString(),
                anyInt(), anyString(), anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startBaywatchServer(any(ContainerOrchestratorCluster.class), anyString(),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startConsulWatches(any(ContainerOrchestratorCluster.class), anyString(), anyInt(),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startRegistrator(any(ContainerOrchestratorCluster.class), anyString(), any(ExitCriteriaModel.class));
    }

    private Set<String> getPrivateIps(Stack stack) {
        Set<String> ips = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            ips.add(instanceMetaData.getPrivateIp());
        }
        return ips;
    }

}