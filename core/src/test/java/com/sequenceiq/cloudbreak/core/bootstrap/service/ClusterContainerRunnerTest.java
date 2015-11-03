package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestratorCluster;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.LogVolumePath;
import com.sequenceiq.cloudbreak.orchestrator.security.KerberosConfiguration;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@RunWith(MockitoJUnitRunner.class)
public class ClusterContainerRunnerTest {

    @Mock
    private StackRepository stackRepository;

    @Mock
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private MockContainerOrchestrator mockContainerOrchestrator;

    @Mock
    private CancelledMockContainerOrchestrator cancelledMockContainerOrchestrator;

    @Mock
    private FailedMockContainerOrchestrator failedMockContainerOrchestrator;

    @Mock
    private ContainerConfigService containerConfigService;

    @InjectMocks
    private ClusterContainerRunner underTest;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(containerConfigService, "ambariDockerImageName", "sequence/testcont:0.1.1");
        ReflectionTestUtils.setField(containerConfigService, "registratorDockerImageName", "sequence/testcont:0.1.1");
        ReflectionTestUtils.setField(containerConfigService, "consulWatchPlugnDockerImageName", "sequence/testcont:0.1.1");
        ReflectionTestUtils.setField(containerConfigService, "postgresDockerImageName", "sequence/testcont:0.1.1");
        ReflectionTestUtils.setField(containerConfigService, "kerberosDockerImageName", "sequence/testcont:0.1.1");
        ReflectionTestUtils.setField(containerConfigService, "baywatchServerDockerImageName", "sequence/testcont:0.1.1");
        ReflectionTestUtils.setField(containerConfigService, "baywatchClientDockerImageName", "sequence/testcont:0.1.1");
        ReflectionTestUtils.setField(containerConfigService, "logrotateDockerImageName", "sequence/testcont:0.1.1");
    }

    @Test
    public void runClusterContainersWhenSecurityEnabled() throws CloudbreakException, CloudbreakOrchestratorFailedException,
            CloudbreakOrchestratorCancelledException {
        ReflectionTestUtils.setField(underTest, "baywatchEnabled", false);
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster(TestUtil.blueprint(), stack, 1L));
        Cluster cluster = new Cluster();
        cluster.setSecure(true);
        ProvisioningContext provisioningContext = new ProvisioningContext.Builder().setDefaultParams(1L, CloudPlatform.AZURE).build();

        when(containerOrchestratorResolver.get()).thenReturn(mockContainerOrchestrator);
        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString(), anyString()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", "/cert/1"));
        when(clusterService.retrieveClusterByStackId(anyLong())).thenReturn(cluster);

        underTest.runClusterContainers(provisioningContext);

        verify(mockContainerOrchestrator, times(1)).startAmbariServer(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ContainerConfig.class), anyString(), any(LogVolumePath.class), eq(true), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startKerberosServer(any(ContainerOrchestratorCluster.class),
                any(ContainerConfig.class), any(LogVolumePath.class), any(KerberosConfiguration.class), any(ExitCriteriaModel.class));
    }

    @Test
    public void runClusterContainersWhenBaywatchEnabled() throws CloudbreakException, CloudbreakOrchestratorFailedException,
            CloudbreakOrchestratorCancelledException {
        ReflectionTestUtils.setField(underTest, "baywatchEnabled", true);
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster(TestUtil.blueprint(), stack, 1L));
        ProvisioningContext provisioningContext = new ProvisioningContext.Builder().setDefaultParams(1L, CloudPlatform.AZURE).build();

        when(containerOrchestratorResolver.get()).thenReturn(mockContainerOrchestrator);
        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString(), anyString()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", "/cert/1"));
        when(clusterService.retrieveClusterByStackId(anyLong())).thenReturn(new Cluster());

        underTest.runClusterContainers(provisioningContext);

        verify(mockContainerOrchestrator, times(1)).startAmbariAgents(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class), anyString(),
                any(LogVolumePath.class), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startAmbariServer(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ContainerConfig.class), anyString(), any(LogVolumePath.class), anyBoolean(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startLogrotate(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startBaywatchClients(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class), anyString(),
                any(LogVolumePath.class), anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startBaywatchServer(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startConsulWatches(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(LogVolumePath.class), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startRegistrator(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class), any(ExitCriteriaModel
                .class));
    }

    @Test(expected = CloudbreakException.class)
    public void runClusterContainersWhenContainerRunnerFailed()
            throws CloudbreakException, CloudbreakOrchestratorFailedException, CloudbreakOrchestratorCancelledException {
        ReflectionTestUtils.setField(underTest, "baywatchEnabled", true);
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster(TestUtil.blueprint(), stack, 1L));
        Cluster cluster = new Cluster();
        cluster.setSecure(false);

        ProvisioningContext provisioningContext = new ProvisioningContext.Builder().setDefaultParams(1L, CloudPlatform.AZURE).build();

        when(containerOrchestratorResolver.get()).thenReturn(new FailedMockContainerOrchestrator());
        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString(), anyString()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", "/cert/1"));
        when(clusterService.retrieveClusterByStackId(1L)).thenReturn(cluster);

        underTest.runClusterContainers(provisioningContext);
    }

    @Test(expected = CancellationException.class)
    public void runClusterContainersWhenContainerRunnerCancelled()
            throws CloudbreakException, CloudbreakOrchestratorFailedException, CloudbreakOrchestratorCancelledException {
        ReflectionTestUtils.setField(underTest, "baywatchEnabled", true);
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster(TestUtil.blueprint(), stack, 1L));
        ProvisioningContext provisioningContext = new ProvisioningContext.Builder().setDefaultParams(1L, CloudPlatform.AZURE).build();
        Cluster cluster = new Cluster();
        cluster.setSecure(false);

        when(containerOrchestratorResolver.get()).thenReturn(new CancelledMockContainerOrchestrator());
        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString(), anyString()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", "/cert/1"));
        when(clusterService.retrieveClusterByStackId(1L)).thenReturn(cluster);

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
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString(), anyString()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", "/cert/1"));
        when(clusterService.retrieveClusterByStackId(anyLong())).thenReturn(new Cluster());

        underTest.runClusterContainers(provisioningContext);

        verify(mockContainerOrchestrator, times(1)).startAmbariAgents(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class), anyString(),
                any(LogVolumePath.class), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startAmbariServer(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ContainerConfig.class), anyString(), any(LogVolumePath.class), anyBoolean(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startBaywatchClients(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class), anyString(),
                any(LogVolumePath.class), anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startBaywatchServer(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startConsulWatches(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(LogVolumePath.class), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startLogrotate(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startRegistrator(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ExitCriteriaModel.class));
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
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString(), anyString()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", "/cert/1"));
        when(clusterService.retrieveClusterByStackId(anyLong())).thenReturn(new Cluster());

        underTest.runClusterContainers(provisioningContext);

        verify(mockContainerOrchestrator, times(1)).startAmbariAgents(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class), anyString(),
                any(LogVolumePath.class), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startAmbariServer(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ContainerConfig.class), anyString(), any(LogVolumePath.class), anyBoolean(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startBaywatchClients(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class), anyString(),
                any(LogVolumePath.class), anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startBaywatchServer(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startConsulWatches(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(LogVolumePath.class), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startLogrotate(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startRegistrator(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ExitCriteriaModel.class));
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
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString(), anyString()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", "/cert/1"));

        underTest.addClusterContainers(context);

        verify(mockContainerOrchestrator, times(1)).startAmbariAgents(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                anyString(), any(LogVolumePath.class), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startAmbariServer(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ContainerConfig.class), anyString(), any(LogVolumePath.class), anyBoolean(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startBaywatchClients(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class), anyString(),
                any(LogVolumePath.class), anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startBaywatchServer(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startConsulWatches(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(LogVolumePath.class), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startLogrotate(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startRegistrator(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ExitCriteriaModel.class));
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
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString(), anyString()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", "/cert/1"));

        underTest.addClusterContainers(context);
    }

    @Test(expected = CancellationException.class)
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
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString(), anyString()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", "/cert/1"));

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
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString(), anyString()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", "/cert/1"));

        underTest.addClusterContainers(context);

        verify(mockContainerOrchestrator, times(1)).startAmbariAgents(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class), anyString(),
                any(LogVolumePath.class), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startAmbariServer(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ContainerConfig.class), anyString(), any(LogVolumePath.class), anyBoolean(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startBaywatchClients(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class), anyString(),
                any(LogVolumePath.class), anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startBaywatchServer(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startConsulWatches(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(LogVolumePath.class), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startLogrotate(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startRegistrator(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ExitCriteriaModel.class));
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
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString(), anyString()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", "/cert/1"));

        underTest.addClusterContainers(context);

        verify(mockContainerOrchestrator, times(1)).startAmbariAgents(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class), anyString(),
                any(LogVolumePath.class), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startAmbariServer(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ContainerConfig.class), anyString(), any(LogVolumePath.class), anyBoolean(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startBaywatchClients(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class), anyString(),
                any(LogVolumePath.class), anyString(), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startBaywatchServer(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startConsulWatches(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(LogVolumePath.class), any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(1)).startLogrotate(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ExitCriteriaModel.class));
        verify(mockContainerOrchestrator, times(0)).startRegistrator(any(ContainerOrchestratorCluster.class), any(ContainerConfig.class),
                any(ExitCriteriaModel.class));
    }

    private Set<String> getPrivateIps(Stack stack) {
        Set<String> ips = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            ips.add(instanceMetaData.getPrivateIp());
        }
        return ips;
    }

}