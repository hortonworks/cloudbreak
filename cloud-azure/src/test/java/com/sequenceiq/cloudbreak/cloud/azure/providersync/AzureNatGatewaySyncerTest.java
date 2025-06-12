package com.sequenceiq.cloudbreak.cloud.azure.providersync;

import static com.sequenceiq.cloudbreak.cloud.model.OutboundType.NOT_DEFINED;
import static com.sequenceiq.cloudbreak.cloud.model.OutboundType.USER_ASSIGNED_NATGATEWAY;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_NAT_GATEWAY;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_NETWORK;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_SUBNET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.network.models.Subnet;
import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureConstants;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.NetworkAttributes;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AzureNatGatewaySyncerTest {

    @Spy
    private AzureCloudResourceService azureCloudResourceService;

    @InjectMocks
    private AzureNatGatewaySyncer underTest;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private AzureClient azureClient;

    @Mock
    private Subnet subnet;

    @Mock
    private AzureOutboundManager azureOutboundManager;

    @BeforeEach
    void setUp() {
        lenient().when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
    }

    @Test
    void testPlatformShouldReturnAzure() {
        assertEquals(AzureConstants.PLATFORM, underTest.platform());
    }

    @Test
    void testVariantShouldReturnAzure() {
        assertEquals(AzureConstants.VARIANT, underTest.variant());
    }

    @Test
    void testGetResourceTypeShouldReturnAzureNatGateway() {
        assertEquals(AZURE_NAT_GATEWAY, underTest.getResourceType());
    }

    @Test
    void testSyncWhenNatGatewayExists() {
        // Given
        String natGatewayId = "/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/natGateways/nat1";
        String networkName = "network1";
        String resourceGroupName = "rg1";
        String subnetName = "subnet1";

        CloudResource natGateway = createCloudResource("nat1", AZURE_NAT_GATEWAY);
        CloudResource network = createCloudResource(networkName, AZURE_NETWORK);
        CloudResource subnetResource = createCloudResource(subnetName, AZURE_SUBNET);
        List<CloudResource> resources = List.of(natGateway, network, subnetResource);

        when(azureClient.getSubnetProperties(resourceGroupName, networkName, subnetName)).thenReturn(subnet);
        when(subnet.natGatewayId()).thenReturn(natGatewayId);
        when(subnet.id()).thenReturn("/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/virtualNetworks/network1/subnets/subnet1");
        when(azureOutboundManager.updateNetworkOutbound(network, USER_ASSIGNED_NATGATEWAY))
                .thenReturn(new CloudResourceStatus(network, ResourceStatus.UPDATED));

        // When
        List<CloudResourceStatus> result = underTest.sync(authenticatedContext, resources);

        // Then
        assertEquals(2, result.size());
        assertEquals(ResourceStatus.CREATED, result.getFirst().getStatus());
        assertEquals(ResourceStatus.UPDATED, result.get(1).getStatus());
        verify(azureClient, times(1)).getSubnetProperties(resourceGroupName, networkName, subnetName);
        verify(azureOutboundManager, times(1)).updateNetworkOutbound(network, USER_ASSIGNED_NATGATEWAY);
    }

    @Test
    void testSyncWhenNatGatewayDeleted() {
        // Given
        String networkName = "network1";
        String resourceGroupName = "rg1";
        String subnetName = "subnet1";

        CloudResource natGateway = createCloudResource("nat1", AZURE_NAT_GATEWAY);
        CloudResource network = createCloudResource(networkName, AZURE_NETWORK);
        CloudResource subnetResource = createCloudResource(subnetName, AZURE_SUBNET);
        List<CloudResource> resources = List.of(natGateway, network, subnetResource);

        when(azureClient.getSubnetProperties(resourceGroupName, networkName, subnetName)).thenReturn(subnet);
        when(subnet.natGatewayId()).thenReturn(null);
        when(subnet.id()).thenReturn("/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/virtualNetworks/network1/subnets/subnet1");
        when(azureOutboundManager.updateNetworkOutbound(network, NOT_DEFINED))
                .thenReturn(new CloudResourceStatus(network, ResourceStatus.UPDATED));

        // When
        List<CloudResourceStatus> result = underTest.sync(authenticatedContext, resources);

        // Then
        assertEquals(2, result.size());
        assertEquals(ResourceStatus.DELETED, result.getFirst().getStatus());
        assertEquals(ResourceStatus.UPDATED, result.get(1).getStatus());
        verify(azureClient, times(1)).getSubnetProperties(resourceGroupName, networkName, subnetName);
        verify(azureOutboundManager, times(1)).updateNetworkOutbound(network, NOT_DEFINED);
    }

    @Test
    void testSyncWhenSubnetNotFound() {
        // Given
        String networkName = "network1";
        String resourceGroupName = "rg1";
        String subnetName = "subnet1";

        CloudResource natGateway = createCloudResource("nat1", AZURE_NAT_GATEWAY);
        CloudResource network = createCloudResource(networkName, AZURE_NETWORK);
        CloudResource subnetResource = createCloudResource(subnetName, AZURE_SUBNET);
        List<CloudResource> resources = List.of(natGateway, network, subnetResource);

        when(azureClient.getSubnetProperties(resourceGroupName, networkName, subnetName)).thenReturn(null);

        // When
        List<CloudResourceStatus> result = underTest.sync(authenticatedContext, resources);

        // Then
        assertTrue(result.isEmpty());
        verify(azureClient, times(1)).getSubnetProperties(resourceGroupName, networkName, subnetName);
    }

    @Test
    void testSyncWhenNoSubnetFound() {
        // Given
        String networkName = "network1";
        String resourceGroupName = "rg1";
        String subnetName = "subnet1";

        CloudResource network = createCloudResource(networkName, AZURE_NETWORK);
        CloudResource subnetResource = createCloudResource(subnetName, AZURE_SUBNET);
        List<CloudResource> resources = List.of(network, subnetResource);

        when(azureClient.getSubnetProperties(resourceGroupName, networkName, subnetName)).thenReturn(null);

        // When
        List<CloudResourceStatus> result = underTest.sync(authenticatedContext, resources);

        // Then
        assertTrue(result.isEmpty());
        verify(azureClient, times(1)).getSubnetProperties(resourceGroupName, networkName, subnetName);
    }

    @Test
    void testSyncWhenNatGatewayFound() {
        // Given
        String natGatewayId = "/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/natGateways/nat1";
        String networkName = "network1";
        String resourceGroupName = "rg1";
        String subnetName = "subnet1";

        CloudResource network = createCloudResource(networkName, AZURE_NETWORK);
        CloudResource subnetResource = createCloudResource(subnetName, AZURE_SUBNET);
        List<CloudResource> resources = List.of(network, subnetResource);

        when(azureClient.getSubnetProperties(resourceGroupName, networkName, subnetName)).thenReturn(subnet);
        when(subnet.natGatewayId()).thenReturn(natGatewayId);
        when(subnet.id()).thenReturn("/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/virtualNetworks/network1/subnets/subnet1");
        when(azureOutboundManager.updateNetworkOutbound(network, USER_ASSIGNED_NATGATEWAY))
                .thenReturn(new CloudResourceStatus(network, ResourceStatus.UPDATED));

        // When
        List<CloudResourceStatus> result = underTest.sync(authenticatedContext, resources);

        // Then
        assertEquals(2, result.size());
        CloudResourceStatus foundResource = result.getFirst();
        assertEquals(ResourceStatus.CREATED, foundResource.getStatus());
        assertEquals("nat1", foundResource.getCloudResource().getName());
        assertEquals(AZURE_NAT_GATEWAY, foundResource.getCloudResource().getType());
        assertEquals(natGatewayId, foundResource.getCloudResource().getReference());
        assertEquals(ResourceStatus.UPDATED, result.get(1).getStatus());
        verify(azureClient, times(1)).getSubnetProperties(resourceGroupName, networkName, subnetName);
        verify(azureOutboundManager, times(1)).updateNetworkOutbound(network, USER_ASSIGNED_NATGATEWAY);
    }

    @Test
    void testSyncWhenNoNatGatewayFound() {
        // Given
        String networkName = "network1";
        String resourceGroupName = "rg1";
        String subnetName = "subnet1";

        CloudResource network = createCloudResource(networkName, AZURE_NETWORK);
        CloudResource subnetResource = createCloudResource(subnetName, AZURE_SUBNET);
        List<CloudResource> resources = List.of(network, subnetResource);

        when(azureClient.getSubnetProperties(resourceGroupName, networkName, subnetName)).thenReturn(subnet);
        when(subnet.natGatewayId()).thenReturn(null);
        when(subnet.id()).thenReturn("/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/virtualNetworks/network1/subnets/subnet1");

        // When
        List<CloudResourceStatus> result = underTest.sync(authenticatedContext, resources);

        // Then
        assertTrue(result.isEmpty());
        verify(azureClient, times(1)).getSubnetProperties(resourceGroupName, networkName, subnetName);
    }

    @Test
    void testSyncWhenEmptyResourcesProvided() {
        // Given
        List<CloudResource> resources = new ArrayList<>();

        // When
        List<CloudResourceStatus> result = underTest.sync(authenticatedContext, resources);

        // Then
        assertTrue(result.isEmpty());
        verify(azureClient, never()).getSubnetProperties(any(), any(), any());
        verify(azureCloudResourceService, never()).buildCloudResource(any(), any(), any());
    }

    @Test
    void testShouldSyncWithMatchingResourceType() {
        // Given
        CloudResource natGateway = createCloudResource("nat1", AZURE_NAT_GATEWAY);
        CloudResource network = createCloudResource("network1", AZURE_NETWORK);
        List<CloudResource> resources = List.of(natGateway, network);

        // When
        boolean shouldSync = underTest.shouldSync(authenticatedContext, resources);

        // Then
        assertTrue(shouldSync);
    }

    @Test
    void testShouldSyncWithoutMatchingResource() {
        // Given
        CloudResource network = createCloudResource("network1", AZURE_NETWORK);
        List<CloudResource> resources = List.of(network);
        when(azureOutboundManager.shouldSyncForOutbound(resources)).thenReturn(false);

        // When
        boolean shouldSync = underTest.shouldSync(authenticatedContext, resources);

        // Then
        assertFalse(shouldSync);
    }

    @Test
    void testShouldSyncWithEmptyResources() {
        // Given
        List<CloudResource> resources = List.of();
        when(azureOutboundManager.shouldSyncForOutbound(resources)).thenReturn(false);

        // When
        boolean shouldSync = underTest.shouldSync(authenticatedContext, resources);

        // Then
        assertFalse(shouldSync);
    }

    @Test
    void testSyncWhenExistingNatGatewayWithSameReference() {
        // Given
        String natGatewayId = "/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/natGateways/nat1";
        String networkName = "network1";
        String resourceGroupName = "rg1";
        String subnetName = "subnet1";

        CloudResource existingNatGateway = CloudResource.builder()
                .withName("nat1")
                .withType(AZURE_NAT_GATEWAY)
                .withStatus(CommonStatus.CREATED)
                .withReference(natGatewayId)
                .build();
        CloudResource network = createCloudResource(networkName, AZURE_NETWORK);
        CloudResource subnetResource = createCloudResource(subnetName, AZURE_SUBNET);
        List<CloudResource> resources = List.of(existingNatGateway, network, subnetResource);

        when(azureClient.getSubnetProperties(resourceGroupName, networkName, subnetName)).thenReturn(subnet);
        when(subnet.natGatewayId()).thenReturn(natGatewayId);
        when(subnet.id()).thenReturn("/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/virtualNetworks/network1/subnets/subnet1");
        when(azureOutboundManager.updateNetworkOutbound(network, USER_ASSIGNED_NATGATEWAY))
                .thenReturn(new CloudResourceStatus(network, ResourceStatus.UPDATED));

        // When
        List<CloudResourceStatus> result = underTest.sync(authenticatedContext, resources);

        // Then
        assertEquals(2, result.size());
        CloudResourceStatus status = result.getFirst();
        assertEquals(ResourceStatus.CREATED, status.getStatus());
        assertEquals(natGatewayId, status.getCloudResource().getReference());
        assertEquals(ResourceStatus.UPDATED, result.get(1).getStatus());
        verify(azureClient, times(1)).getSubnetProperties(resourceGroupName, networkName, subnetName);
        verify(azureOutboundManager, times(1)).updateNetworkOutbound(network, USER_ASSIGNED_NATGATEWAY);
    }

    @Test
    void testSyncWhenExistingNatGatewayWithDifferentReference() {
        // Given
        String oldNatGatewayId = "/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/natGateways/nat1";
        String newNatGatewayId = "/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/natGateways/nat2";
        String networkName = "network1";
        String resourceGroupName = "rg1";
        String subnetName = "subnet1";

        CloudResource existingNatGateway = CloudResource.builder()
                .withName("nat1")
                .withType(AZURE_NAT_GATEWAY)
                .withStatus(CommonStatus.CREATED)
                .withReference(oldNatGatewayId)
                .build();
        CloudResource network = createCloudResource(networkName, AZURE_NETWORK);
        CloudResource subnetResource = createCloudResource(subnetName, AZURE_SUBNET);
        List<CloudResource> resources = List.of(existingNatGateway, network, subnetResource);

        when(azureClient.getSubnetProperties(resourceGroupName, networkName, subnetName)).thenReturn(subnet);
        when(subnet.natGatewayId()).thenReturn(newNatGatewayId);
        when(subnet.id()).thenReturn("/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/virtualNetworks/network1/subnets/subnet1");
        when(azureOutboundManager.updateNetworkOutbound(network, USER_ASSIGNED_NATGATEWAY))
                .thenReturn(new CloudResourceStatus(network, ResourceStatus.UPDATED));

        // When
        List<CloudResourceStatus> result = underTest.sync(authenticatedContext, resources);

        // Then
        assertEquals(2, result.size());
        CloudResourceStatus status = result.getFirst();
        assertEquals(ResourceStatus.CREATED, status.getStatus());
        assertEquals(newNatGatewayId, status.getCloudResource().getReference());
        assertEquals(ResourceStatus.UPDATED, result.get(1).getStatus());
        verify(azureClient, times(1)).getSubnetProperties(resourceGroupName, networkName, subnetName);
        verify(azureOutboundManager, times(1)).updateNetworkOutbound(network, USER_ASSIGNED_NATGATEWAY);
        verify(azureCloudResourceService, times(1)).buildCloudResource("nat2", newNatGatewayId, AZURE_NAT_GATEWAY);
    }

    private CloudResource createCloudResource(String name, ResourceType type) {
        CloudResource.Builder builder = CloudResource.builder()
                .withName(name)
                .withType(type);
        if (type == AZURE_NETWORK) {
            builder.withParameters(Map.of("attributes", createAttributes(name, "rg1")));
        }
        return builder.build();
    }

    private NetworkAttributes createAttributes(String networkId, String networkResourceGroupName) {
        NetworkAttributes networkAttributes = new NetworkAttributes();
        networkAttributes.setResourceGroupName(networkResourceGroupName);
        networkAttributes.setNetworkId(networkId);
        return networkAttributes;
    }

}