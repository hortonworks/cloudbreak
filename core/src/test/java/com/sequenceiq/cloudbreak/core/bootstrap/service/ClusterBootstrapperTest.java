package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.service.HostDiscoveryService;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostBootstrapApiCheckerTask;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostClusterAvailabilityCheckerTask;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.context.HostBootstrapApiContext;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.context.HostOrchestratorClusterContext;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.orchestrator.OrchestratorService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@RunWith(MockitoJUnitRunner.class)
public class ClusterBootstrapperTest {

    @Mock
    private StackService stackService;

    @Mock
    private OrchestratorService orchestratorService;

    @Mock
    private PollingService<HostBootstrapApiContext> hostBootstrapApiPollingService;

    @Mock
    private HostBootstrapApiCheckerTask hostBootstrapApiCheckerTask;

    @Mock
    private PollingService<HostOrchestratorClusterContext> hostClusterAvailabilityPollingService;

    @Mock
    private HostClusterAvailabilityCheckerTask hostClusterAvailabilityCheckerTask;

    @Mock
    private ClusterBootstrapperErrorHandler clusterBootstrapperErrorHandler;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private HostDiscoveryService hostDiscoveryService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentProvider;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @InjectMocks
    private ClusterBootstrapper underTest;

    @Mock
    private Stack stack;

    @Mock
    private Image image;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Spy
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private ResourceService resourceService;

    @Mock
    private TransactionService transactionService;

    @Test
    public void shouldUseReachableInstances() throws Exception {
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactionService).required(any(Runnable.class));
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setPrivateIp("1.1.1.1");
        instanceMetaData.setPublicIp("2.2.2.2");
        instanceMetaData.setDiscoveryFQDN("FQDN");
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("master");
        Template template = new Template();
        template.setInstanceType("GATEWAY");
        instanceGroup.setTemplate(template);
        instanceMetaData.setInstanceGroup(instanceGroup);
        when(stack.getId()).thenReturn(1L);
        when(instanceMetaDataService.getReachableInstanceMetadataByStackId(stack.getId())).thenReturn(Set.of(instanceMetaData));
        when(stack.getCustomDomain()).thenReturn("CUSTOM_DOMAIN");
        Cluster cluster = new Cluster();
        cluster.setGateway(new Gateway());
        when(stack.getCluster()).thenReturn(cluster);
        GatewayConfig gatewayConfig = new GatewayConfig("host1", "1.1.1.1", "1.1.1.1", 22, "i-1839", false);
        when(gatewayConfigService.getAllGatewayConfigs(any())).thenReturn(List.of(gatewayConfig));
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(image);

        underTest.bootstrapNewNodes(1L, Set.of("1.1.1.1"), List.of("host1"));

        verify(instanceMetaDataService).getReachableInstanceMetadataByStackId(1L);
        verify(gatewayConfigService).getAllGatewayConfigs(stack);
        verify(componentConfigProviderService).getImage(1L);
        verify(instanceMetaDataService).saveAll(Set.of(instanceMetaData));
        verify(hostOrchestrator, never()).removeDeadSaltMinions(gatewayConfig);
    }

    @Test
    public void testcleanupOldSaltState() throws Exception {
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactionService).required(any(Runnable.class));
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setPrivateIp("1.1.1.1");
        instanceMetaData.setPublicIp("2.2.2.2");
        instanceMetaData.setDiscoveryFQDN("FQDN");
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("master");
        Template template = new Template();
        template.setInstanceType("GATEWAY");
        instanceGroup.setTemplate(template);
        instanceMetaData.setInstanceGroup(instanceGroup);
        when(stack.getId()).thenReturn(1L);
        when(instanceMetaDataService.getReachableInstanceMetadataByStackId(stack.getId())).thenReturn(Set.of(instanceMetaData));
        when(stack.getCustomDomain()).thenReturn("CUSTOM_DOMAIN");
        Cluster cluster = new Cluster();
        cluster.setGateway(new Gateway());
        when(stack.getCluster()).thenReturn(cluster);
        GatewayConfig deadConfig = new GatewayConfig("host1", "1.1.1.1", "1.1.1.1", 22, "i-1839", false);
        GatewayConfig aliveConfig = new GatewayConfig("host2", "1.1.1.2", "1.1.1.2", 22, "i-1839", false);
        when(gatewayConfigService.getAllGatewayConfigs(any())).thenReturn(List.of(deadConfig, aliveConfig));
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(image);

        underTest.bootstrapNewNodes(1L, Set.of("1.1.1.1"), List.of("host1"));

        verify(instanceMetaDataService).getReachableInstanceMetadataByStackId(1L);
        verify(gatewayConfigService).getAllGatewayConfigs(stack);
        verify(componentConfigProviderService).getImage(1L);
        verify(instanceMetaDataService).saveAll(Set.of(instanceMetaData));
        verify(hostOrchestrator, never()).removeDeadSaltMinions(deadConfig);
        verify(hostOrchestrator, times(1)).removeDeadSaltMinions(aliveConfig);
    }
}