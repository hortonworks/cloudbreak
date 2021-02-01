package com.sequenceiq.cloudbreak.cloud.azure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AzureTerminationHelperServiceTest {

    private static final String RESOURCE_GROUP_NAME = "resourceGroupName";

    private static final String NETWORK_INTERFACE_NAME = "networkInterfaceName";

    private static final String PUBLIC_ADDRESS_NAME = "publicAddressName";

    private static final String NETWORK_ID = "networkId";

    private static final String INSTANCE_NAME = "myInstance";

    private static final String AVAILABILITY_SET_NAME = "availabilitySetName";

    private static final String SUBNET_NAME = "subnetName";

    private static final String VOLUMESET_NAME = "volumeSetName";

    private static final String NSG_ID = "securityGroupId";

    private static final String MANAGED_DISK_NAME = "managedDiskName";

    private static final List<String> AVAILABILITY_SET_NAME_LIST = List.of(AVAILABILITY_SET_NAME);

    private static final List<String> PUBLIC_ADDRESS_NAME_LIST = List.of(PUBLIC_ADDRESS_NAME);

    private static final List<String> NETWORK_INTERFACE_NAME_LIST = List.of(NETWORK_INTERFACE_NAME);

    private static final List<String> MANAGED_DISK_NAME_LIST = List.of(MANAGED_DISK_NAME);

    private static final List<String> NSG_ID_LIST = List.of(NSG_ID);

    private static final List<String> NETWORK_ID_LIST = List.of(NETWORK_ID);

    @Mock
    private AzureStorage azureStorage;

    @Mock
    private CloudResourceHelper cloudResourceHelper;

    @Mock
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Mock
    private AzureVirtualMachineService azureVirtualMachineService;

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AzureComputeResourceService azureComputeResourceService;

    @Mock
    private AzureResourceConnector azureResourceConnector;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private AzureCloudResourceService azureCloudResourceService;

    @InjectMocks
    private AzureTerminationHelperService underTest;

    private final List<CloudResource> resourcesToRemoveList = setupCloudResources();

    @BeforeEach
    void setup() {
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(), (CloudStack) any())).thenReturn(RESOURCE_GROUP_NAME);
        when(azureCloudResourceService.getNetworkResources(any())).thenReturn(List.of());
    }

    @Test
    void testWhenDownscaleThenNoAvailabilitySets() {

        List<CloudResource> volumeSets = getCloudResourcesByType(resourcesToRemoveList, ResourceType.AZURE_VOLUMESET);
        underTest.downscale(ac, cloudStack, List.of(), List.of(), resourcesToRemoveList);

        verify(azureUtils).deleteInstances(any(), any());
        verify(azureUtils).waitForDetachNetworkInterfaces(eq(ac), any(), eq(RESOURCE_GROUP_NAME), eq(NETWORK_INTERFACE_NAME_LIST));
        verify(azureUtils).deleteNetworkInterfaces(any(), eq(RESOURCE_GROUP_NAME), eq(NETWORK_INTERFACE_NAME_LIST));
        verify(azureUtils).deletePublicIps(any(), eq(RESOURCE_GROUP_NAME), eq(PUBLIC_ADDRESS_NAME_LIST));
        verify(azureUtils).deleteManagedDisks(any(), eq(RESOURCE_GROUP_NAME), eq(MANAGED_DISK_NAME_LIST));
        verify(azureComputeResourceService).deleteComputeResources(any(), any(), eq(volumeSets), any());
        verify(azureUtils, never()).deleteAvailabilitySets(any(), eq(RESOURCE_GROUP_NAME), anyCollection());
        verify(azureUtils, never()).deleteSecurityGroups(any(), eq(NSG_ID_LIST));
        verify(azureUtils, never()).deleteNetworks(any(), eq(NETWORK_ID_LIST));
    }

    @Test
    void testWhenTerminateThenAlsoAvailabilitySets() {
        List<CloudResource> volumeSets = getCloudResourcesByType(resourcesToRemoveList, ResourceType.AZURE_VOLUMESET);

        underTest.terminate(ac, cloudStack, resourcesToRemoveList);

        verify(azureUtils).deleteInstances(any(), any());
        verify(azureUtils).waitForDetachNetworkInterfaces(eq(ac), any(), eq(RESOURCE_GROUP_NAME), eq(NETWORK_INTERFACE_NAME_LIST));
        verify(azureUtils).deleteNetworkInterfaces(any(), eq(RESOURCE_GROUP_NAME), eq(NETWORK_INTERFACE_NAME_LIST));
        verify(azureUtils).deletePublicIps(any(), eq(RESOURCE_GROUP_NAME), eq(PUBLIC_ADDRESS_NAME_LIST));
        verify(azureUtils).deleteManagedDisks(any(), eq(RESOURCE_GROUP_NAME), eq(MANAGED_DISK_NAME_LIST));
        verify(azureUtils).deleteAvailabilitySets(any(), any(), eq(AVAILABILITY_SET_NAME_LIST));

        verify(azureComputeResourceService).deleteComputeResources(any(), any(), eq(volumeSets), any());
        verify(azureUtils).deleteSecurityGroups(any(), eq(NSG_ID_LIST));
        verify(azureUtils).deleteNetworks(any(), eq(NETWORK_ID_LIST));
    }

    @Test
    void testWhenDeleteWholeDeploymentThenLoadBalancersAreRemovedBeforePublicIps() {
        underTest.terminate(ac, cloudStack, resourcesToRemoveList);

        InOrder inOrder = inOrder(azureUtils);
        inOrder.verify(azureUtils).deleteLoadBalancers(any(), any(), any());
        inOrder.verify(azureUtils).deletePublicIps(any(), any(), any());
    }

    private List<CloudResource> setupCloudResources() {
        return List.of(
                createCloudResource(ResourceType.AZURE_INSTANCE, INSTANCE_NAME),
                createCloudResource(ResourceType.AZURE_NETWORK_INTERFACE, NETWORK_INTERFACE_NAME),
                createCloudResource(ResourceType.AZURE_PUBLIC_IP, PUBLIC_ADDRESS_NAME),
                createCloudResource(ResourceType.AZURE_DISK, MANAGED_DISK_NAME),
                createCloudResource(ResourceType.AZURE_AVAILABILITY_SET, AVAILABILITY_SET_NAME),
                createCloudResource(ResourceType.AZURE_SUBNET, SUBNET_NAME),
                createCloudResource(ResourceType.AZURE_VOLUMESET, VOLUMESET_NAME),
                createCloudResource(ResourceType.AZURE_RESOURCE_GROUP, RESOURCE_GROUP_NAME),
                createCloudResource(ResourceType.AZURE_SECURITY_GROUP, "anNSG", NSG_ID),
                createCloudResource(ResourceType.AZURE_NETWORK, "aNetwork", NETWORK_ID)
                );
    }

    private CloudResource createCloudResource(ResourceType resourceType, String name) {
        return createCloudResource(resourceType, name, "");
    }

    private CloudResource createCloudResource(ResourceType resourceType, String name, String reference) {
        return CloudResource.builder()
                .type(resourceType)
                .status(CommonStatus.CREATED)
                .name(name)
                .reference(reference)
                .params(Map.of())
                .build();
    }

    private List<CloudResource> getCloudResourcesByType(List<CloudResource> resourcesToRemove, ResourceType resourceType) {
        return resourcesToRemove.stream()
                .filter(cloudResource -> resourceType.equals(cloudResource.getType()))
                .collect(Collectors.toList());
    }
}
