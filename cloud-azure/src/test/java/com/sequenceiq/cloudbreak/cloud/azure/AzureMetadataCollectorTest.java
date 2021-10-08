package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NicIPConfiguration;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.loadbalancer.AzureLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

@RunWith(MockitoJUnitRunner.class)
public class AzureMetadataCollectorTest {

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

    private static final String SUBNET_NAME = "subnet-1";

    private static final String PRIVATE_IP = "10.11.12.13";

    private static final String PUBLIC_IP = "192.168.1.1";

    private static final int FAULT_DOMAIN_COUNT = 1;

    private static final String REGION = "EU-WEST1";

    private static final String PLATFORM = "AZURE";

    private static final String LOCALITY_INDICATOR = "/AZURE/EU-WEST1/resourceGroup-1/test-instance-group/1";

    private static final String SECOND_PUBLIC_IP = "10.32.13.1";

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

    @Test
    public void testCollectShouldReturnsTheAllVmMetadata() {
        List<CloudResource> resources = Collections.emptyList();
        List<CloudInstance> vms = createVms();
        CloudResource cloudResource = createCloudResource();
        CloudContext cloudContext = mock(CloudContext.class);
        Map<String, VirtualMachine> machines = getMachines();

        when(azureUtils.getTemplateResource(resources)).thenReturn(cloudResource);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        when(azureUtils.getPrivateInstanceId(any(), any(), any()))
                .thenReturn(INSTANCE_1)
                .thenReturn(INSTANCE_2)
                .thenReturn(INSTANCE_3);
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
        NicIPConfiguration nicIPConfiguration = mock(NicIPConfiguration.class);
        when(virtualMachine.name()).thenReturn(name);
        when(virtualMachine.getPrimaryNetworkInterface()).thenReturn(networkInterface);
        when(networkInterface.primaryIPConfiguration()).thenReturn(nicIPConfiguration);
        when(networkInterface.primaryPrivateIP()).thenReturn(PRIVATE_IP);
        when(nicIPConfiguration.subnetName()).thenReturn(SUBNET_NAME);
        return virtualMachine;
    }

    private List<CloudInstance> createVms() {
        return List.of(
                createCloudInstance(INSTANCE_1, PRIVATE_ID_1),
                createCloudInstance(INSTANCE_2, PRIVATE_ID_2),
                createCloudInstance(INSTANCE_3, PRIVATE_ID_3));
    }

    private CloudInstance createCloudInstance(String instanceId, Long privateId) {
        InstanceTemplate instanceTemplate = new InstanceTemplate(null, INSTANCE_GROUP_NAME, privateId, Collections.emptyList(), null, Collections.emptyMap(),
                null, null, TemporaryStorage.ATTACHED_VOLUMES, 0L);
        return new CloudInstance(instanceId, instanceTemplate, null, "subnet-1", "az1");
    }

    private CloudResource createCloudResource() {
        return new CloudResource.Builder()
                .type(ResourceType.AZURE_NETWORK)
                .status(CommonStatus.CREATED)
                .name(RESOURCE_GROUP_NAME)
                .params(Collections.emptyMap())
                .build();
    }

    @Test
    public void testCollectSinglePublicLoadBalancer() {
        List<CloudResource> resources = new ArrayList<>();
        CloudResource cloudResource = createCloudResource();

        when(authenticatedContext.getCloudContext()).thenReturn(mockCloudContext);
        when(mockCloudContext.getName()).thenReturn(RESOURCE_GROUP_NAME);

        when(azureUtils.getTemplateResource(resources)).thenReturn(cloudResource);
        when(azureUtils.getStackName(any())).thenReturn(STACK_NAME);

        final String loadBalancerName = AzureLoadBalancer.getLoadBalancerName(LoadBalancerType.PUBLIC, STACK_NAME);
        when(azureClient.getLoadBalancerIps(RESOURCE_GROUP_NAME, loadBalancerName, LoadBalancerType.PUBLIC))
            .thenReturn(List.of(PUBLIC_IP));

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureLbMetadataCollector.getParameters(any(), anyString(), anyString())).thenReturn(Map.of());

        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PUBLIC), resources);

        assertEquals(1, result.size());
        assertEquals(PUBLIC_IP, result.get(0).getIp());
        assertEquals(LoadBalancerType.PUBLIC, result.get(0).getType());
    }

    @Test
    public void testCollectSinglePublicLoadBalancerWithMultipleIps() {
        List<CloudResource> resources = new ArrayList<>();
        CloudResource cloudResource = createCloudResource();

        when(authenticatedContext.getCloudContext()).thenReturn(mockCloudContext);
        when(mockCloudContext.getName()).thenReturn(RESOURCE_GROUP_NAME);

        when(azureUtils.getTemplateResource(resources)).thenReturn(cloudResource);
        when(azureUtils.getStackName(any())).thenReturn(STACK_NAME);

        final String loadBalancerName = AzureLoadBalancer.getLoadBalancerName(LoadBalancerType.PUBLIC, STACK_NAME);
        when(azureClient.getLoadBalancerIps(RESOURCE_GROUP_NAME, loadBalancerName, LoadBalancerType.PUBLIC))
                .thenReturn(List.of(PUBLIC_IP, SECOND_PUBLIC_IP));

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureLbMetadataCollector.getParameters(any(), anyString(), anyString())).thenReturn(Map.of());

        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PUBLIC), resources);


        // We're retrieving only one Public IP address by design
        assertEquals(1, result.size());
        assertEquals(LoadBalancerType.PUBLIC, result.get(0).getType());
        final String resultIpAddress = result.get(0).getIp();
        assertTrue(PUBLIC_IP.equals(resultIpAddress) || SECOND_PUBLIC_IP.equals(resultIpAddress));
    }

    @Test
    public void testCollectPublicAndPrivateLoadBalancer() {
        List<CloudResource> resources = new ArrayList<>();
        CloudResource cloudResource = createCloudResource();

        when(authenticatedContext.getCloudContext()).thenReturn(mockCloudContext);
        when(mockCloudContext.getName()).thenReturn(RESOURCE_GROUP_NAME);

        when(azureUtils.getTemplateResource(resources)).thenReturn(cloudResource);
        when(azureUtils.getStackName(any())).thenReturn(STACK_NAME);

        final String publicLoadBalancerName = AzureLoadBalancer.getLoadBalancerName(LoadBalancerType.PUBLIC, STACK_NAME);
        when(azureClient.getLoadBalancerIps(RESOURCE_GROUP_NAME, publicLoadBalancerName, LoadBalancerType.PUBLIC))
                .thenReturn(List.of(PUBLIC_IP));

        final String privateLoadBalancerName = AzureLoadBalancer.getLoadBalancerName(LoadBalancerType.PRIVATE, STACK_NAME);
        when(azureClient.getLoadBalancerIps(RESOURCE_GROUP_NAME, privateLoadBalancerName, LoadBalancerType.PRIVATE))
                .thenReturn(List.of(PRIVATE_IP));

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
    public void testCollectPrivateLoadBalancer() {
        List<CloudResource> resources = new ArrayList<>();
        CloudResource cloudResource = createCloudResource();

        when(authenticatedContext.getCloudContext()).thenReturn(mockCloudContext);
        when(mockCloudContext.getName()).thenReturn(RESOURCE_GROUP_NAME);

        when(azureUtils.getTemplateResource(resources)).thenReturn(cloudResource);
        when(azureUtils.getStackName(any())).thenReturn(STACK_NAME);

        final String privateLoadBalancerName = AzureLoadBalancer.getLoadBalancerName(LoadBalancerType.PRIVATE, STACK_NAME);
        when(azureClient.getLoadBalancerIps(RESOURCE_GROUP_NAME, privateLoadBalancerName, LoadBalancerType.PRIVATE))
                .thenReturn(List.of(PRIVATE_IP));

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureLbMetadataCollector.getParameters(any(), anyString(), anyString())).thenReturn(Map.of());

        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PRIVATE), resources);

        assertEquals(1, result.size());
        assertEquals(LoadBalancerType.PRIVATE, result.get(0).getType());
        assertEquals(PRIVATE_IP, result.get(0).getIp());
    }

    @Test
    public void testCollectPrivateLoadBalancerWithMultipleIps() {
        List<CloudResource> resources = new ArrayList<>();
        CloudResource cloudResource = createCloudResource();

        when(authenticatedContext.getCloudContext()).thenReturn(mockCloudContext);
        when(mockCloudContext.getName()).thenReturn(RESOURCE_GROUP_NAME);

        when(azureUtils.getTemplateResource(resources)).thenReturn(cloudResource);
        when(azureUtils.getStackName(any())).thenReturn(STACK_NAME);

        final String privateLoadBalancerName = AzureLoadBalancer.getLoadBalancerName(LoadBalancerType.PRIVATE, STACK_NAME);
        when(azureClient.getLoadBalancerIps(RESOURCE_GROUP_NAME, privateLoadBalancerName, LoadBalancerType.PRIVATE))
                .thenReturn(List.of(PRIVATE_IP, "10.23.12.1"));

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureLbMetadataCollector.getParameters(any(), anyString(), anyString())).thenReturn(Map.of());

        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PRIVATE), resources);

        assertEquals(1, result.size());
        assertEquals(LoadBalancerType.PRIVATE, result.get(0).getType());
        assertEquals(PRIVATE_IP, result.get(0).getIp());
    }

    @Test
    public void testCollectLoadBalancerWithNoIps() {
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
    public void testCollectLoadBalancerSkipsMetadataWhenRuntimeExceptionIsThrown() {
        List<CloudResource> resources = new ArrayList<>();
        CloudResource cloudResource = createCloudResource();

        when(authenticatedContext.getCloudContext()).thenReturn(mockCloudContext);
        when(mockCloudContext.getName()).thenReturn(RESOURCE_GROUP_NAME);

        when(azureUtils.getTemplateResource(resources)).thenReturn(cloudResource);
        when(azureUtils.getStackName(any())).thenReturn(STACK_NAME);

        final String privateLoadBalancerName = AzureLoadBalancer.getLoadBalancerName(LoadBalancerType.PRIVATE, STACK_NAME);
        when(azureClient.getLoadBalancerIps(RESOURCE_GROUP_NAME, privateLoadBalancerName, LoadBalancerType.PRIVATE))
                .thenThrow(new RuntimeException());

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);

        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PRIVATE), resources);

        assertEquals(0, result.size());
    }
}