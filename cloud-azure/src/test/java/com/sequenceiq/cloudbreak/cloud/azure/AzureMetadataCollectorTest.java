package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static com.sequenceiq.common.api.type.ResourceType.ARM_TEMPLATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.compute.models.VirtualMachineSizeTypes;
import com.azure.resourcemanager.network.models.NetworkInterface;
import com.azure.resourcemanager.network.models.NicIpConfiguration;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.loadbalancer.AzureLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCheckMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTypeMetadata;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AzureMetadataCollectorTest {

    private static final List<CloudInstance> KNOWN_INSTANCES = Collections.emptyList();

    private static final String INSTANCE_GROUP_NAME = "test-instance-group";

    private static final String INSTANCE_1 = "instance-1";

    private static final String INSTANCE_2 = "instance-2";

    private static final String INSTANCE_3 = "instance-3";

    private static final long PRIVATE_ID_1 = 1L;

    private static final long PRIVATE_ID_2 = 2L;

    private static final long PRIVATE_ID_3 = 3L;

    private static final String RESOURCE_GROUP_NAME = "resourceGroup-1";

    private static final String STACK_NAME = "test-stack";

    private static final Long STACK_ID = 1L;

    private static final String SUBNET_NAME = "subnet-1";

    private static final String PRIVATE_IP = "10.11.12.13";

    private static final String PUBLIC_IP = "192.168.1.1";

    private static final int FAULT_DOMAIN_COUNT = 1;

    private static final String REGION = "EU-WEST1";

    private static final String PLATFORM = "AZURE";

    private static final String LOCALITY_INDICATOR = "/AZURE/EU-WEST1/resourceGroup-1/test-instance-group/1";

    private static final String SECOND_PUBLIC_IP = "10.32.13.1";

    private static final String GATEWAY_PRIVATE_IP = "10.132.113.101";

    private static final String PUBLIC_FRONTEND = "PublicFrontend";

    private static final String SECOND_PUBLIC_FRONTEND = "SecondPublicFrontend";

    private static final String PRIVATE_FRONTEND = "PrivateFrontend";

    private static final String GATEWAY_PRIVATE_FRONTEND = "GatewayPrivateFrontend";

    private static final String RESOURCE_CRN = "resource-crn";

    @InjectMocks
    private AzureMetadataCollector underTest;

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AzureVirtualMachineService azureVirtualMachineService;

    @Mock
    private AzureVmPublicIpProvider azureVmPublicIpProvider;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private AzureClient azureClient;

    @Mock
    private CloudContext mockCloudContext;

    @Mock
    private AzureLoadBalancerMetadataCollector azureLbMetadataCollector;

    @Mock
    private ResourceRetriever resourceRetriever;

    @Test
    void testCollectShouldReturnsTheAllVmMetadata() {
        List<CloudResource> resources = Collections.emptyList();
        List<CloudInstance> vms = createVms();
        CloudResource cloudResource = createCloudResource();
        CloudContext cloudContext = mock(CloudContext.class);
        Map<String, VirtualMachine> machines = getMachines();

        when(azureUtils.getTemplateResource(resources)).thenReturn(cloudResource);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        when(azureVirtualMachineService.getVirtualMachinesByName(eq(azureClient), eq(RESOURCE_GROUP_NAME), anySet())).thenReturn(machines);
        when(azureClient.getFaultDomainNumber(RESOURCE_GROUP_NAME, INSTANCE_1)).thenReturn(FAULT_DOMAIN_COUNT);
        when(azureClient.getFaultDomainNumber(RESOURCE_GROUP_NAME, INSTANCE_2)).thenReturn(FAULT_DOMAIN_COUNT);
        when(azureClient.getFaultDomainNumber(RESOURCE_GROUP_NAME, INSTANCE_3)).thenReturn(FAULT_DOMAIN_COUNT);
        when(azureVmPublicIpProvider.getPublicIp(any())).thenReturn(PUBLIC_IP);
        when(cloudContext.getPlatform()).thenReturn(Platform.platform(PLATFORM));
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region(REGION), null));

        List<CloudVmMetaDataStatus> actual = underTest.collect(authenticatedContext, resources, vms, KNOWN_INSTANCES);

        assertEquals(3, actual.size());
        assertEquals(InstanceStatus.CREATED, actual.get(0).getCloudVmInstanceStatus().getStatus());
        assertEquals(PRIVATE_IP, actual.get(0).getMetaData().getPrivateIp());
        assertEquals(PUBLIC_IP, actual.get(0).getMetaData().getPublicIp());
        assertEquals(LOCALITY_INDICATOR, actual.get(0).getMetaData().getLocalityIndicator());

        assertEquals(InstanceStatus.CREATED, actual.get(1).getCloudVmInstanceStatus().getStatus());
        assertEquals(PRIVATE_IP, actual.get(1).getMetaData().getPrivateIp());
        assertEquals(PUBLIC_IP, actual.get(1).getMetaData().getPublicIp());
        assertEquals(LOCALITY_INDICATOR, actual.get(1).getMetaData().getLocalityIndicator());

        assertEquals(InstanceStatus.CREATED, actual.get(2).getCloudVmInstanceStatus().getStatus());
        assertEquals(PRIVATE_IP, actual.get(2).getMetaData().getPrivateIp());
        assertEquals(PUBLIC_IP, actual.get(2).getMetaData().getPublicIp());
        assertEquals(LOCALITY_INDICATOR, actual.get(2).getMetaData().getLocalityIndicator());
        ArgumentCaptor<Collection> collectionArgumentCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(azureVirtualMachineService, times(1))
                .getVirtualMachinesByName(any(), eq(RESOURCE_GROUP_NAME), collectionArgumentCaptor.capture());
        Collection instanceIds = collectionArgumentCaptor.getValue();
        assertThat(instanceIds).containsExactlyInAnyOrder(INSTANCE_1, INSTANCE_2, INSTANCE_3);
        verify(azureUtils, times(0)).getFullInstanceId(anyString(), anyString(), anyString(), anyString());
    }

    private Map<String, VirtualMachine> getMachines() {
        return Map.of(
                INSTANCE_1, createVirtualMachine(INSTANCE_1),
                INSTANCE_2, createVirtualMachine(INSTANCE_2),
                INSTANCE_3, createVirtualMachine(INSTANCE_3));
    }

    private VirtualMachine createVirtualMachine(String name) {
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        NetworkInterface networkInterface = mock(NetworkInterface.class);
        NicIpConfiguration nicIPConfiguration = mock(NicIpConfiguration.class);
        lenient().when(virtualMachine.name()).thenReturn(name);
        lenient().when(virtualMachine.getPrimaryNetworkInterface()).thenReturn(networkInterface);
        lenient().when(networkInterface.primaryIPConfiguration()).thenReturn(nicIPConfiguration);
        lenient().when(networkInterface.primaryPrivateIP()).thenReturn(PRIVATE_IP);
        lenient().when(nicIPConfiguration.subnetName()).thenReturn(SUBNET_NAME);
        lenient().when(virtualMachine.size()).thenReturn(VirtualMachineSizeTypes.BASIC_A1);
        lenient().when(virtualMachine.id()).thenReturn(String.format("microsoft/%s/%s", RESOURCE_GROUP_NAME, name));
        return virtualMachine;
    }

    private List<CloudInstance> createVms() {
        return List.of(
                createCloudInstance(INSTANCE_1, PRIVATE_ID_1, 1L),
                createCloudInstance(INSTANCE_2, PRIVATE_ID_2, 2L),
                createCloudInstance(INSTANCE_3, PRIVATE_ID_3, 3L));
    }

    private CloudInstance createCloudInstance(String instanceId, Long privateId, Long id) {
        InstanceTemplate instanceTemplate = new InstanceTemplate(null, INSTANCE_GROUP_NAME, privateId, Collections.emptyList(), null, Collections.emptyMap(),
                null, null, TemporaryStorage.ATTACHED_VOLUMES, 0L);
        return new CloudInstance(instanceId, instanceTemplate, null, "subnet-1", "az1",
                Collections.singletonMap(CloudInstance.ID, id));
    }

    private CloudResource createCloudResource() {
        return CloudResource.builder()
                .withType(ResourceType.AZURE_NETWORK)
                .withStatus(CommonStatus.CREATED)
                .withName(RESOURCE_GROUP_NAME)
                .withParameters(Collections.emptyMap())
                .build();
    }

    @Test
    void testCollectSinglePublicLoadBalancer() {
        List<CloudResource> resources = new ArrayList<>();
        CloudResource cloudResource = createCloudResource();

        when(authenticatedContext.getCloudContext()).thenReturn(mockCloudContext);
        when(mockCloudContext.getName()).thenReturn(RESOURCE_GROUP_NAME);

        when(azureUtils.getTemplateResource(resources)).thenReturn(cloudResource);
        when(azureUtils.getStackName(any())).thenReturn(STACK_NAME);

        String loadBalancerName = AzureLoadBalancer.getLoadBalancerName(LoadBalancerType.PUBLIC, STACK_NAME);
        when(azureClient.getLoadBalancerFrontends(RESOURCE_GROUP_NAME, loadBalancerName, LoadBalancerType.PUBLIC))
                .thenReturn(List.of(new AzureLoadBalancerFrontend(PUBLIC_FRONTEND, PUBLIC_IP, LoadBalancerType.PUBLIC)));

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureLbMetadataCollector.getParameters(any(), anyString(), anyString())).thenReturn(Map.of());

        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PUBLIC), resources);

        assertEquals(1, result.size());
        assertEquals(PUBLIC_IP, result.get(0).getIp());
        assertEquals(LoadBalancerType.PUBLIC, result.get(0).getType());
    }

    @Test
    void testCollectSinglePublicLoadBalancerWithMultipleIps() {
        List<CloudResource> resources = new ArrayList<>();
        CloudResource cloudResource = createCloudResource();

        when(authenticatedContext.getCloudContext()).thenReturn(mockCloudContext);
        when(mockCloudContext.getName()).thenReturn(RESOURCE_GROUP_NAME);

        when(azureUtils.getTemplateResource(resources)).thenReturn(cloudResource);
        when(azureUtils.getStackName(any())).thenReturn(STACK_NAME);

        String loadBalancerName = AzureLoadBalancer.getLoadBalancerName(LoadBalancerType.PUBLIC, STACK_NAME);
        when(azureClient.getLoadBalancerFrontends(RESOURCE_GROUP_NAME, loadBalancerName, LoadBalancerType.PUBLIC))
                .thenReturn(List.of(new AzureLoadBalancerFrontend(PUBLIC_FRONTEND, PUBLIC_IP, LoadBalancerType.PUBLIC),
                        new AzureLoadBalancerFrontend(SECOND_PUBLIC_FRONTEND, SECOND_PUBLIC_IP, LoadBalancerType.PUBLIC)));

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureLbMetadataCollector.getParameters(any(), anyString(), anyString())).thenReturn(Map.of());

        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PUBLIC), resources);

        // We're retrieving only one Public IP address by design
        assertEquals(1, result.size());
        assertEquals(LoadBalancerType.PUBLIC, result.get(0).getType());
        String resultIpAddress = result.get(0).getIp();
        assertTrue(PUBLIC_IP.equals(resultIpAddress) || SECOND_PUBLIC_IP.equals(resultIpAddress));
    }

    @Test
    void testCollectPublicAndPrivateLoadBalancer() {
        List<CloudResource> resources = new ArrayList<>();
        CloudResource cloudResource = createCloudResource();

        when(authenticatedContext.getCloudContext()).thenReturn(mockCloudContext);
        when(mockCloudContext.getName()).thenReturn(RESOURCE_GROUP_NAME);

        when(azureUtils.getTemplateResource(resources)).thenReturn(cloudResource);
        when(azureUtils.getStackName(any())).thenReturn(STACK_NAME);

        String publicLoadBalancerName = AzureLoadBalancer.getLoadBalancerName(LoadBalancerType.PUBLIC, STACK_NAME);
        when(azureClient.getLoadBalancerFrontends(RESOURCE_GROUP_NAME, publicLoadBalancerName, LoadBalancerType.PUBLIC))
                .thenReturn(List.of(new AzureLoadBalancerFrontend(PUBLIC_FRONTEND, PUBLIC_IP, LoadBalancerType.PUBLIC)));

        String privateLoadBalancerName = AzureLoadBalancer.getLoadBalancerName(LoadBalancerType.PRIVATE, STACK_NAME);
        when(azureClient.getLoadBalancerFrontends(RESOURCE_GROUP_NAME, privateLoadBalancerName, LoadBalancerType.PRIVATE))
                .thenReturn(List.of(new AzureLoadBalancerFrontend(PRIVATE_FRONTEND, PRIVATE_IP, LoadBalancerType.PRIVATE)));

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureLbMetadataCollector.getParameters(any(), anyString(), anyString())).thenReturn(Map.of());

        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext,
                List.of(LoadBalancerType.PUBLIC, LoadBalancerType.PRIVATE), resources);

        assertEquals(2, result.size());
        Optional<CloudLoadBalancerMetadata> publicLoadBalancerMetadata = result.stream()
                .filter(metadata -> metadata.getType() == LoadBalancerType.PUBLIC)
                .findAny();
        assertTrue(publicLoadBalancerMetadata.isPresent());
        assertEquals(PUBLIC_IP, publicLoadBalancerMetadata.get().getIp());

        Optional<CloudLoadBalancerMetadata> privateLoadBalancerMetadata = result.stream()
                .filter(metadata -> metadata.getType() == LoadBalancerType.PRIVATE)
                .findAny();
        assertTrue(privateLoadBalancerMetadata.isPresent());
        assertEquals(PRIVATE_IP, privateLoadBalancerMetadata.get().getIp());
    }

    @Test
    void testCollectPrivateLoadBalancer() {
        List<CloudResource> resources = new ArrayList<>();
        CloudResource cloudResource = createCloudResource();

        when(authenticatedContext.getCloudContext()).thenReturn(mockCloudContext);
        when(mockCloudContext.getName()).thenReturn(RESOURCE_GROUP_NAME);

        when(azureUtils.getTemplateResource(resources)).thenReturn(cloudResource);
        when(azureUtils.getStackName(any())).thenReturn(STACK_NAME);

        String privateLoadBalancerName = AzureLoadBalancer.getLoadBalancerName(LoadBalancerType.PRIVATE, STACK_NAME);
        when(azureClient.getLoadBalancerFrontends(RESOURCE_GROUP_NAME, privateLoadBalancerName, LoadBalancerType.PRIVATE))
                .thenReturn(List.of(new AzureLoadBalancerFrontend(PRIVATE_FRONTEND, PRIVATE_IP, LoadBalancerType.PRIVATE)));

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureLbMetadataCollector.getParameters(any(), anyString(), anyString())).thenReturn(Map.of());

        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PRIVATE), resources);

        assertEquals(1, result.size());
        assertEquals(LoadBalancerType.PRIVATE, result.get(0).getType());
        assertEquals(PRIVATE_IP, result.get(0).getIp());
    }

    @Test
    void testCollectGatewayPrivateLoadBalancer() {
        List<CloudResource> resources = new ArrayList<>();
        CloudResource cloudResource = createCloudResource();

        when(authenticatedContext.getCloudContext()).thenReturn(mockCloudContext);
        when(mockCloudContext.getName()).thenReturn(RESOURCE_GROUP_NAME);

        when(azureUtils.getTemplateResource(resources)).thenReturn(cloudResource);
        when(azureUtils.getStackName(any())).thenReturn(STACK_NAME);

        String privateLoadBalancerName = AzureLoadBalancer.getLoadBalancerName(LoadBalancerType.PRIVATE, STACK_NAME);
        when(azureClient.getLoadBalancerFrontends(RESOURCE_GROUP_NAME, privateLoadBalancerName, LoadBalancerType.PRIVATE))
                .thenReturn(List.of(new AzureLoadBalancerFrontend(PRIVATE_FRONTEND, PRIVATE_IP, LoadBalancerType.PRIVATE)));

        when(azureClient.getLoadBalancerFrontends(RESOURCE_GROUP_NAME, privateLoadBalancerName, LoadBalancerType.GATEWAY_PRIVATE))
                .thenReturn(List.of(new AzureLoadBalancerFrontend(GATEWAY_PRIVATE_FRONTEND, GATEWAY_PRIVATE_IP, LoadBalancerType.GATEWAY_PRIVATE)));

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureLbMetadataCollector.getParameters(any(), anyString(), anyString())).thenReturn(Map.of());

        List<CloudLoadBalancerMetadata> result =
                underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.GATEWAY_PRIVATE, LoadBalancerType.PRIVATE), resources);

        assertEquals(2, result.size());
        assertThat(result).anyMatch(lbm -> LoadBalancerType.GATEWAY_PRIVATE == lbm.getType() && GATEWAY_PRIVATE_IP.equals(lbm.getIp()))
                .anyMatch(lbm -> LoadBalancerType.PRIVATE == lbm.getType() && PRIVATE_IP.equals(lbm.getIp()));
    }

    @Test
    void testCollectPrivateLoadBalancerWithMultipleIps() {
        List<CloudResource> resources = new ArrayList<>();
        CloudResource cloudResource = createCloudResource();

        when(authenticatedContext.getCloudContext()).thenReturn(mockCloudContext);
        when(mockCloudContext.getName()).thenReturn(RESOURCE_GROUP_NAME);

        when(azureUtils.getTemplateResource(resources)).thenReturn(cloudResource);
        when(azureUtils.getStackName(any())).thenReturn(STACK_NAME);

        String privateLoadBalancerName = AzureLoadBalancer.getLoadBalancerName(LoadBalancerType.PRIVATE, STACK_NAME);
        when(azureClient.getLoadBalancerFrontends(RESOURCE_GROUP_NAME, privateLoadBalancerName, LoadBalancerType.PRIVATE))
                .thenReturn(List.of(new AzureLoadBalancerFrontend(PRIVATE_FRONTEND, PRIVATE_IP, LoadBalancerType.PRIVATE),
                        new AzureLoadBalancerFrontend(GATEWAY_PRIVATE_FRONTEND, "10.23.12.1", LoadBalancerType.GATEWAY_PRIVATE)));

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureLbMetadataCollector.getParameters(any(), anyString(), anyString())).thenReturn(Map.of());

        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PRIVATE), resources);

        assertEquals(1, result.size());
        assertEquals(LoadBalancerType.PRIVATE, result.get(0).getType());
        assertEquals(PRIVATE_IP, result.get(0).getIp());
    }

    @Test
    void testCollectLoadBalancerWithNoIps() {
        List<CloudResource> resources = new ArrayList<>();
        CloudResource cloudResource = createCloudResource();

        when(authenticatedContext.getCloudContext()).thenReturn(mockCloudContext);
        when(mockCloudContext.getName()).thenReturn(RESOURCE_GROUP_NAME);

        when(azureUtils.getTemplateResource(resources)).thenReturn(cloudResource);
        when(azureUtils.getStackName(any())).thenReturn(STACK_NAME);

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);

        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(
                authenticatedContext,
                List.of(LoadBalancerType.PRIVATE, LoadBalancerType.PUBLIC),
                resources);

        assertEquals(0, result.size());
    }

    @Test
    void testCollectLoadBalancerSkipsMetadataWhenRuntimeExceptionIsThrown() {
        List<CloudResource> resources = new ArrayList<>();
        CloudResource cloudResource = createCloudResource();

        when(authenticatedContext.getCloudContext()).thenReturn(mockCloudContext);
        when(mockCloudContext.getName()).thenReturn(RESOURCE_GROUP_NAME);

        when(azureUtils.getTemplateResource(resources)).thenReturn(cloudResource);
        when(azureUtils.getStackName(any())).thenReturn(STACK_NAME);

        String privateLoadBalancerName = AzureLoadBalancer.getLoadBalancerName(LoadBalancerType.PRIVATE, STACK_NAME);
        when(azureClient.getLoadBalancerFrontends(RESOURCE_GROUP_NAME, privateLoadBalancerName, LoadBalancerType.PRIVATE))
                .thenThrow(new RuntimeException());

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);

        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PRIVATE), resources);

        assertEquals(0, result.size());
    }

    @Test
    void testCollectInstanceTypes() {
        CloudResource cloudResource = CloudResource.builder().withName(RESOURCE_GROUP_NAME).withType(ARM_TEMPLATE).withStatus(CREATED)
                .withParameters(new HashMap<>()).build();
        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getId()).thenReturn(STACK_ID);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(resourceRetriever.findByStatusAndTypeAndStack(eq(CREATED), eq(ARM_TEMPLATE), eq(STACK_ID))).thenReturn(Optional.of(cloudResource));
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        Map<String, VirtualMachine> machines = getMachines();
        when(azureVirtualMachineService.getVirtualMachinesByName(eq(azureClient), eq(RESOURCE_GROUP_NAME), eq(List.of(INSTANCE_1, INSTANCE_2, INSTANCE_3))))
                .thenReturn(machines);

        InstanceTypeMetadata result = underTest.collectInstanceTypes(authenticatedContext, List.of(INSTANCE_1, INSTANCE_2, INSTANCE_3));

        verify(azureVirtualMachineService, times(1))
                .getVirtualMachinesByName(eq(azureClient), eq(RESOURCE_GROUP_NAME), eq(List.of(INSTANCE_1, INSTANCE_2, INSTANCE_3)));
        Map<String, String> instanceTypes = result.getInstanceTypes();
        assertThat(instanceTypes).hasSize(3);
        assertThat(instanceTypes).containsEntry(INSTANCE_1, VirtualMachineSizeTypes.BASIC_A1.toString());
        assertThat(instanceTypes).containsEntry(INSTANCE_2, VirtualMachineSizeTypes.BASIC_A1.toString());
        assertThat(instanceTypes).containsEntry(INSTANCE_3, VirtualMachineSizeTypes.BASIC_A1.toString());
    }

    @Test
    void testCollectCdpInstances() {
        InstanceCheckMetadata instanceCheckMetadata1 = mock(InstanceCheckMetadata.class);
        InstanceCheckMetadata instanceCheckMetadata2 = mock(InstanceCheckMetadata.class);
        CloudStack cloudStack = mock(CloudStack.class);
        List<String> knownInstanceIds = mock(List.class);
        when(azureVirtualMachineService.collectCdpInstances(authenticatedContext, RESOURCE_CRN, cloudStack, knownInstanceIds))
                .thenReturn(List.of(instanceCheckMetadata1, instanceCheckMetadata2));

        List<InstanceCheckMetadata> result = underTest.collectCdpInstances(authenticatedContext, RESOURCE_CRN, cloudStack, knownInstanceIds);

        assertThat(result).containsExactlyInAnyOrder(instanceCheckMetadata1, instanceCheckMetadata2);
    }
}
