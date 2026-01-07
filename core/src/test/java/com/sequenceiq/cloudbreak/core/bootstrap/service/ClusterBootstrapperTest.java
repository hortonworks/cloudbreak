package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.PricingCache;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.HostDiscoveryService;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostBootstrapApiCheckerTask;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostClusterAvailabilityCheckerTask;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.context.HostBootstrapApiContext;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.context.HostOrchestratorClusterContext;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.StackUpdaterService;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.orchestrator.OrchestratorService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class ClusterBootstrapperTest {

    private static final String NODE_NAME = "nodeName";

    private static final String INSTANCE_ID = "instanceId";

    private static final String DOMAIN = "CUSTOM_DOMAIN";

    @Mock
    private StackDtoService stackDtoService;

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
    private StackDto stack;

    @Mock
    private Image image;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private ResourceService resourceService;

    @Mock
    private SecurityConfigService securityConfigService;

    @Mock
    private StackUpdaterService stackUpdaterService;

    @Mock
    private SaltBootstrapVersionChecker saltBootstrapVersionChecker;

    @Mock
    private Resource volumeSet;

    @Mock
    private VolumeSetAttributes volumeSetAttributes;

    @Mock
    private ClusterNodeNameGenerator clusterNodeNameGenerator;

    @Spy
    private List<PricingCache> pricingCaches = new ArrayList<>();

    @Mock
    private PricingCache pricingCache;

    @Mock
    private CredentialClientService credentialClientService;

    @Mock
    private CredentialToExtendedCloudCredentialConverter credentialToExtendedCloudCredentialConverter;

    @Captor
    private ArgumentCaptor<BootstrapParams> bootstrapParamsCaptor;

    @Test
    public void shouldUseReachableInstances() throws Exception {
        when(stackDtoService.getById(1L)).thenReturn(stack);
        InstanceMetaData instanceMetaData = newInstanceMetaData();
        InstanceGroup instanceGroup = newMasterInstanceGroup();
        instanceMetaData.setInstanceGroup(instanceGroup);
        when(stack.getId()).thenReturn(1L);
        when(instanceMetaDataService.getReachableInstanceMetadataByStackId(stack.getId())).thenReturn(Set.of(instanceMetaData));
        when(stack.getCustomDomain()).thenReturn(DOMAIN);
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());
        pricingCaches.add(pricingCache);
        when(pricingCache.getCloudPlatform()).thenReturn(CloudPlatform.AWS);
        when(pricingCache.getCpuCountForInstanceType(any(), any(), any())).thenReturn(Optional.of(16));
        when(stack.getInstanceGroupDtos()).thenReturn(List.of(new InstanceGroupDto(instanceGroup, null)));
        Cluster cluster = new Cluster();
        cluster.setGateway(new Gateway());
        when(stack.getCluster()).thenReturn(cluster);
        GatewayConfig gatewayConfig = GatewayConfig.builder()
                        .withConnectionAddress("host1")
                        .withPublicAddress("1.1.1.1")
                        .withPrivateAddress("1.1.1.1")
                        .withGatewayPort(22)
                        .withInstanceId("i-1839")
                        .withKnoxGatewayEnabled(false)
                        .build();
        when(gatewayConfigService.getAllGatewayConfigs(any())).thenReturn(List.of(gatewayConfig));
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(image);
        when(hostClusterAvailabilityPollingService.pollWithAbsoluteTimeout(any(), any(), anyInt(), anyLong()))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build());

        underTest.bootstrapNewNodes(1L, Set.of("1.1.1.1"));

        verify(instanceMetaDataService).getReachableInstanceMetadataByStackId(1L);
        verify(gatewayConfigService).getAllGatewayConfigs(stack);
        verify(componentConfigProviderService).getImage(1L);
        verify(instanceMetaDataService).saveAll(Set.of(instanceMetaData));
        verify(hostOrchestrator, never()).removeDeadSaltMinions(gatewayConfig);
        verify(hostOrchestrator).bootstrapNewNodes(any(), any(), any(), any(), bootstrapParamsCaptor.capture(), any(), anyBoolean());
        BootstrapParams bootstrapParams = bootstrapParamsCaptor.getValue();
        assertEquals(12, bootstrapParams.getMasterWorkerThreads());
    }

    @Test
    public void shouldUseDefaultMasterWorkerThreadCountWhenInstanceCpuCountQueryFails() throws Exception {
        when(stackDtoService.getById(1L)).thenReturn(stack);
        InstanceMetaData instanceMetaData = newInstanceMetaData();
        InstanceGroup instanceGroup = newMasterInstanceGroup();
        instanceMetaData.setInstanceGroup(instanceGroup);
        when(stack.getId()).thenReturn(1L);
        when(instanceMetaDataService.getReachableInstanceMetadataByStackId(stack.getId())).thenReturn(Set.of(instanceMetaData));
        when(stack.getCustomDomain()).thenReturn(DOMAIN);
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());
        pricingCaches.add(pricingCache);
        when(pricingCache.getCloudPlatform()).thenReturn(CloudPlatform.AWS);
        doThrow(new RuntimeException("Failed to get cpu count.")).when(pricingCache).getCpuCountForInstanceType(any(), any(), any());
        when(stack.getInstanceGroupDtos()).thenReturn(List.of(new InstanceGroupDto(instanceGroup, null)));
        Cluster cluster = new Cluster();
        cluster.setGateway(new Gateway());
        when(stack.getCluster()).thenReturn(cluster);
        GatewayConfig gatewayConfig = GatewayConfig.builder()
                .withConnectionAddress("host1")
                .withPublicAddress("1.1.1.1")
                .withPrivateAddress("1.1.1.1")
                .withGatewayPort(22)
                .withInstanceId("i-1839")
                .withKnoxGatewayEnabled(false)
                .build();
        when(gatewayConfigService.getAllGatewayConfigs(any())).thenReturn(List.of(gatewayConfig));
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(image);
        when(hostClusterAvailabilityPollingService.pollWithAbsoluteTimeout(any(), any(), anyInt(), anyLong()))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build());

        underTest.bootstrapNewNodes(1L, Set.of("1.1.1.1"));

        verify(instanceMetaDataService).getReachableInstanceMetadataByStackId(1L);
        verify(gatewayConfigService).getAllGatewayConfigs(stack);
        verify(componentConfigProviderService).getImage(1L);
        verify(instanceMetaDataService).saveAll(Set.of(instanceMetaData));
        verify(hostOrchestrator, never()).removeDeadSaltMinions(gatewayConfig);
        verify(hostOrchestrator).bootstrapNewNodes(any(), any(), any(), any(), bootstrapParamsCaptor.capture(), any(), anyBoolean());
        BootstrapParams bootstrapParams = bootstrapParamsCaptor.getValue();
        assertNull(bootstrapParams.getMasterWorkerThreads());
    }

    @Test
    public void shouldInitializeDiscoveryFQDNForVolumeSets() throws Exception {
        when(stackDtoService.getById(1L)).thenReturn(stack);
        when(stack.getDiskResources()).thenReturn(List.of(volumeSet));
        when(volumeSet.getInstanceId()).thenReturn(INSTANCE_ID);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setPrivateIp("1.1.1.1");
        instanceMetaData.setPublicIp("2.2.2.2");
        instanceMetaData.setInstanceId(INSTANCE_ID);
        InstanceGroup instanceGroup = newMasterInstanceGroup();
        instanceMetaData.setInstanceGroup(instanceGroup);
        when(stack.getId()).thenReturn(1L);
        when(instanceMetaDataService.getReachableInstanceMetadataByStackId(stack.getId())).thenReturn(Set.of(instanceMetaData));
        when(stack.getCustomDomain()).thenReturn(DOMAIN);
        Cluster cluster = new Cluster();
        cluster.setGateway(new Gateway());
        when(stack.getCluster()).thenReturn(cluster);
        GatewayConfig gatewayConfig = GatewayConfig.builder()
                .withConnectionAddress("host1")
                .withPublicAddress("1.1.1.1")
                .withPrivateAddress("1.1.1.1")
                .withGatewayPort(22)
                .withInstanceId("i-1839")
                .withKnoxGatewayEnabled(false)
                .build();
        when(gatewayConfigService.getAllGatewayConfigs(any())).thenReturn(List.of(gatewayConfig));
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(image);
        when(hostClusterAvailabilityPollingService.pollWithAbsoluteTimeout(any(), any(), anyInt(), anyLong()))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build());
        when(clusterNodeNameGenerator.getNodeNameForInstanceMetadata(any(), any(), any(), any())).thenReturn(NODE_NAME);
        when(resourceAttributeUtil.getTypedAttributes(any(), any())).thenReturn(Optional.of(volumeSetAttributes));
        ArgumentCaptor<String> fqdnCaptor = ArgumentCaptor.forClass(String.class);

        underTest.bootstrapNewNodes(1L, Set.of("1.1.1.1"));

        verify(volumeSetAttributes).setDiscoveryFQDN(fqdnCaptor.capture());
        assertEquals(NODE_NAME + "." + DOMAIN, fqdnCaptor.getValue());
        verify(resourceService, times(1)).saveAll(List.of(volumeSet));
    }

    @Test
    public void doNotThrowDuplicateKeyNullIfVolumeResourceDoesNotHaveInstanceId() throws Exception {
        when(stackDtoService.getById(1L)).thenReturn(stack);
        InstanceMetaData instanceMetaData = newInstanceMetaData();
        InstanceGroup instanceGroup = newMasterInstanceGroup();
        instanceMetaData.setInstanceGroup(instanceGroup);
        when(stack.getId()).thenReturn(1L);
        when(instanceMetaDataService.getReachableInstanceMetadataByStackId(stack.getId())).thenReturn(Set.of(instanceMetaData));
        when(stack.getCustomDomain()).thenReturn(DOMAIN);
        Cluster cluster = new Cluster();
        cluster.setGateway(new Gateway());
        when(stack.getCluster()).thenReturn(cluster);
        GatewayConfig gatewayConfig = GatewayConfig.builder()
                .withConnectionAddress("host1")
                .withPublicAddress("1.1.1.1")
                .withPrivateAddress("1.1.1.1")
                .withGatewayPort(22)
                .withInstanceId("i-1839")
                .withKnoxGatewayEnabled(false)
                .build();
        when(gatewayConfigService.getAllGatewayConfigs(any())).thenReturn(List.of(gatewayConfig));
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(image);
        when(hostClusterAvailabilityPollingService.pollWithAbsoluteTimeout(any(), any(), anyInt(), anyLong()))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build());

        underTest.bootstrapNewNodes(1L, Set.of("1.1.1.1"));

        verify(instanceMetaDataService).getReachableInstanceMetadataByStackId(1L);
        verify(gatewayConfigService).getAllGatewayConfigs(stack);
        verify(componentConfigProviderService).getImage(1L);
        verify(instanceMetaDataService).saveAll(Set.of(instanceMetaData));
        verify(hostOrchestrator, never()).removeDeadSaltMinions(gatewayConfig);
    }

    @Test
    public void testCleanupOldSaltState() throws Exception {
        when(stackDtoService.getById(1L)).thenReturn(stack);
        InstanceMetaData instanceMetaData = newInstanceMetaData();
        InstanceGroup instanceGroup = newMasterInstanceGroup();
        instanceMetaData.setInstanceGroup(instanceGroup);
        when(stack.getId()).thenReturn(1L);
        when(instanceMetaDataService.getReachableInstanceMetadataByStackId(stack.getId())).thenReturn(Set.of(instanceMetaData));
        when(stack.getCustomDomain()).thenReturn(DOMAIN);
        Cluster cluster = new Cluster();
        cluster.setGateway(new Gateway());
        when(stack.getCluster()).thenReturn(cluster);
        GatewayConfig deadConfig = GatewayConfig.builder()
                .withConnectionAddress("host1")
                .withPublicAddress("1.1.1.1")
                .withPrivateAddress("1.1.1.1")
                .withGatewayPort(22)
                .withInstanceId("i-1839")
                .withKnoxGatewayEnabled(false)
                .build();
        GatewayConfig aliveConfig = GatewayConfig.builder()
                .withConnectionAddress("host2")
                .withPublicAddress("1.1.1.2")
                .withPrivateAddress("1.1.1.2")
                .withGatewayPort(22)
                .withInstanceId("i-1839")
                .withKnoxGatewayEnabled(false)
                .build();
        when(gatewayConfigService.getAllGatewayConfigs(any())).thenReturn(List.of(deadConfig, aliveConfig));
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(image);
        when(hostClusterAvailabilityPollingService.pollWithAbsoluteTimeout(any(), any(), anyInt(), anyLong()))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build());

        underTest.bootstrapNewNodes(1L, Set.of("1.1.1.1"));

        verify(instanceMetaDataService).getReachableInstanceMetadataByStackId(1L);
        verify(gatewayConfigService).getAllGatewayConfigs(stack);
        verify(componentConfigProviderService).getImage(1L);
        verify(instanceMetaDataService).saveAll(Set.of(instanceMetaData));
        verify(hostOrchestrator, never()).removeDeadSaltMinions(deadConfig);
        verify(hostOrchestrator, times(1)).removeDeadSaltMinions(aliveConfig);
    }

    @Test
    public void testCleanupOldSaltStateBothMasterRepaired() throws Exception {
        when(stackDtoService.getById(1L)).thenReturn(stack);
        InstanceMetaData instanceMetaData = newInstanceMetaData();
        InstanceGroup instanceGroup = newMasterInstanceGroup();
        instanceMetaData.setInstanceGroup(instanceGroup);
        InstanceMetaData instanceMetaData2 = newInstanceMetaData("1.1.1.2", "3.3.3.3");
        instanceMetaData2.setInstanceGroup(instanceGroup);

        when(stack.getId()).thenReturn(1L);
        when(instanceMetaDataService.getReachableInstanceMetadataByStackId(stack.getId())).thenReturn(Set.of(instanceMetaData, instanceMetaData2));
        when(stack.getCustomDomain()).thenReturn(DOMAIN);
        Cluster cluster = new Cluster();
        cluster.setGateway(new Gateway());
        when(stack.getCluster()).thenReturn(cluster);
        GatewayConfig deadConfig1 = GatewayConfig.builder()
                .withConnectionAddress("host1")
                .withPublicAddress("1.1.1.1")
                .withPrivateAddress("1.1.1.1")
                .withGatewayPort(22)
                .withInstanceId("i-1839")
                .withKnoxGatewayEnabled(false)
                .build();
        GatewayConfig deadConfig2 = GatewayConfig.builder()
                .withConnectionAddress("host2")
                .withPublicAddress("1.1.1.2")
                .withPrivateAddress("1.1.1.2")
                .withGatewayPort(22)
                .withInstanceId("i-1839")
                .withKnoxGatewayEnabled(false)
                .build();
        when(gatewayConfigService.getAllGatewayConfigs(any())).thenReturn(List.of(deadConfig1, deadConfig2));
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(image);
        when(hostClusterAvailabilityPollingService.pollWithAbsoluteTimeout(any(), any(), anyInt(), anyLong()))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build());

        underTest.bootstrapNewNodes(1L, Set.of("1.1.1.1", "1.1.1.2"));

        verify(instanceMetaDataService).getReachableInstanceMetadataByStackId(1L);
        verify(gatewayConfigService).getAllGatewayConfigs(stack);
        verify(componentConfigProviderService).getImage(1L);
        verify(instanceMetaDataService).saveAll(Set.of(instanceMetaData, instanceMetaData2));
        verify(hostOrchestrator, never()).removeDeadSaltMinions(deadConfig1);
        verify(hostOrchestrator, never()).removeDeadSaltMinions(deadConfig2);
    }

    private InstanceMetaData newInstanceMetaData() {
        return newInstanceMetaData("1.1.1.1", "2.2.2.2");
    }

    private InstanceMetaData newInstanceMetaData(String privateIp, String publicIp) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setPrivateIp(privateIp);
        instanceMetaData.setPublicIp(publicIp);
        instanceMetaData.setDiscoveryFQDN("FQDN");
        return instanceMetaData;
    }

    private InstanceGroup newMasterInstanceGroup() {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("master");
        instanceGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        Template template = new Template();
        template.setInstanceType("m5x.large");
        instanceGroup.setTemplate(template);
        return instanceGroup;
    }
}
