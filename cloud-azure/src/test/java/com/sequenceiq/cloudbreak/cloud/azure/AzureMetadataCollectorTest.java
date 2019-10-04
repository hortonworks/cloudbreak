package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NicIPConfiguration;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.common.api.type.CommonStatus;
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
        when(azureVmPublicIpProvider.getPublicIp(eq(azureClient), eq(azureUtils), any(), eq(RESOURCE_GROUP_NAME))).thenReturn(PUBLIC_IP);
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
                null, null);
        return new CloudInstance(instanceId, instanceTemplate, null);
    }

    private CloudResource createCloudResource() {
        return new CloudResource.Builder()
                .type(ResourceType.AZURE_NETWORK)
                .status(CommonStatus.CREATED)
                .name(RESOURCE_GROUP_NAME)
                .params(Collections.emptyMap())
                .build();
    }

}