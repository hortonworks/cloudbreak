package com.sequenceiq.cloudbreak.cloud.azure.providersync;

import static com.azure.resourcemanager.network.models.IpAllocationMethod.DYNAMIC;
import static com.azure.resourcemanager.network.models.IpAllocationMethod.STATIC;
import static com.sequenceiq.common.api.type.OutboundType.PUBLIC_IP;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_NETWORK;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_PUBLIC_IP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.network.models.PublicIPSkuType;
import com.azure.resourcemanager.network.models.PublicIpAddress;
import com.azure.resourcemanager.network.models.PublicIpAddressSku;
import com.azure.resourcemanager.network.models.PublicIpAddressSkuName;
import com.sequenceiq.cloudbreak.cloud.azure.AzureConstants;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.SkuAttributes;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AzurePublicIpSyncerTest {

    @InjectMocks
    private AzurePublicIpSyncer underTest;

    @Mock
    private AzureOutboundManager azureOutboundManager;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private AzureClient azureClient;

    @Mock
    private PublicIpAddress publicIpAddress;

    @Mock
    private PublicIPSkuType publicIPSkuType;

    @Mock
    private PublicIpAddressSku publicIpAddressSku;

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
    void testGetResourceTypeShouldReturnAzurePublicIp() {
        assertEquals(AZURE_PUBLIC_IP, underTest.getResourceType());
    }

    @Test
    void testShouldSyncWhenPublicIpResourcesExistWithBasicSku() {
        // Given
        CloudResource publicIp = createPublicIpResource("publicIp1", "basic");
        List<CloudResource> resources = List.of(publicIp);

        // When
        boolean result = underTest.shouldSync(authenticatedContext, resources);

        // Then
        assertTrue(result);
        verify(azureOutboundManager, never()).shouldSyncForOutbound(anyList());
    }

    @Test
    void testShouldSyncWhenPublicIpResourcesExistWithStandardSku() {
        // Given
        CloudResource publicIp = createPublicIpResource("publicIp1", "standard");
        List<CloudResource> resources = List.of(publicIp);

        // When
        boolean result = underTest.shouldSync(authenticatedContext, resources);

        // Then
        assertFalse(result);
        verify(azureOutboundManager, never()).shouldSyncForOutbound(anyList());
    }

    @Test
    void testShouldSyncWhenPublicIpResourcesExistWithNullSku() {
        // Given
        CloudResource publicIp = createPublicIpResource("publicIp1", null);
        List<CloudResource> resources = List.of(publicIp);

        // When
        boolean result = underTest.shouldSync(authenticatedContext, resources);

        // Then
        assertTrue(result);
        verify(azureOutboundManager, never()).shouldSyncForOutbound(anyList());
    }

    @Test
    void testShouldSyncWhenPublicIpResourcesExistWithoutSkuAttributes() {
        // Given
        CloudResource publicIp = createCloudResource("publicIp1", AZURE_PUBLIC_IP);
        List<CloudResource> resources = List.of(publicIp);

        // When
        boolean result = underTest.shouldSync(authenticatedContext, resources);

        // Then
        assertTrue(result);
        verify(azureOutboundManager, never()).shouldSyncForOutbound(anyList());
    }

    @Test
    void testShouldSyncWhenNoPublicIpResourcesExist() {
        // Given
        CloudResource network = createCloudResource("network1", AZURE_NETWORK);
        List<CloudResource> resources = List.of(network);
        when(azureOutboundManager.shouldSyncForOutbound(resources)).thenReturn(true);

        // When
        boolean result = underTest.shouldSync(authenticatedContext, resources);

        // Then
        assertTrue(result);
        verify(azureOutboundManager, times(1)).shouldSyncForOutbound(resources);
    }

    @Test
    void testShouldSyncWhenNoPublicIpResourcesExistAndOutboundManagerReturnsFalse() {
        // Given
        CloudResource network = createCloudResource("network1", AZURE_NETWORK);
        List<CloudResource> resources = List.of(network);
        when(azureOutboundManager.shouldSyncForOutbound(resources)).thenReturn(false);

        // When
        boolean result = underTest.shouldSync(authenticatedContext, resources);

        // Then
        assertFalse(result);
        verify(azureOutboundManager, times(1)).shouldSyncForOutbound(resources);
    }

    @Test
    void testShouldSyncWhenEmptyResourceList() {
        // Given
        List<CloudResource> resources = List.of();
        when(azureOutboundManager.shouldSyncForOutbound(resources)).thenReturn(false);

        // When
        boolean result = underTest.shouldSync(authenticatedContext, resources);

        // Then
        assertFalse(result);
        verify(azureOutboundManager, times(1)).shouldSyncForOutbound(resources);
    }

    @Test
    void testSyncWhenPublicIpResourcesExist() {
        // Given
        String publicIpId = "/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/publicIPAddresses/publicIp1";
        CloudResource publicIpResource = createCloudResourceWithReference("publicIp1", AZURE_PUBLIC_IP, publicIpId);
        CloudResource network = createCloudResource("network1", AZURE_NETWORK);
        List<CloudResource> resources = List.of(publicIpResource, network);

        when(azureClient.getPublicIpAddresses(anySet(), anyString())).thenReturn(List.of(publicIpAddress));
        when(publicIpAddress.id()).thenReturn(publicIpId);
        when(publicIpAddress.sku()).thenReturn(publicIPSkuType);
        when(publicIPSkuType.sku()).thenReturn(publicIpAddressSku);
        when(publicIpAddressSku.name()).thenReturn(PublicIpAddressSkuName.STANDARD);
        when(publicIpAddress.ipAllocationMethod()).thenReturn(STATIC);
        when(azureOutboundManager.updateNetworkOutbound(network, PUBLIC_IP))
                .thenReturn(new CloudResourceStatus(network, ResourceStatus.UPDATED));

        // When
        List<CloudResourceStatus> result = underTest.sync(authenticatedContext, resources);

        // Then
        assertEquals(2, result.size());
        assertEquals(ResourceStatus.CREATED, result.get(0).getStatus());
        assertEquals(publicIpResource, result.get(0).getCloudResource());
        assertEquals(ResourceStatus.UPDATED, result.get(1).getStatus());
        assertEquals(network, result.get(1).getCloudResource());

        // Verify SKU attributes were set
        SkuAttributes skuAttributes = publicIpResource.getTypedAttributes(SkuAttributes.class, SkuAttributes::new);
        assertEquals("Standard", skuAttributes.getSku());
        assertEquals("Static", skuAttributes.getIpAllocationMethod());

        verify(azureClient, times(1)).getPublicIpAddresses(anySet(), anyString());
        verify(azureOutboundManager, times(1)).updateNetworkOutbound(network, PUBLIC_IP);
    }

    @Test
    void testSyncWhenPublicIpResourcesExistButSkuIsNull() {
        // Given
        String publicIpId = "/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/publicIPAddresses/publicIp1";
        CloudResource publicIpResource = createCloudResourceWithReference("publicIp1", AZURE_PUBLIC_IP, publicIpId);
        CloudResource network = createCloudResource("network1", AZURE_NETWORK);
        List<CloudResource> resources = List.of(publicIpResource, network);

        when(azureClient.getPublicIpAddresses(anySet(), anyString())).thenReturn(List.of(publicIpAddress));
        when(publicIpAddress.id()).thenReturn(publicIpId);
        when(publicIpAddress.sku()).thenReturn(null);
        when(azureOutboundManager.updateNetworkOutbound(network, PUBLIC_IP))
                .thenReturn(new CloudResourceStatus(network, ResourceStatus.UPDATED));

        // When
        List<CloudResourceStatus> result = underTest.sync(authenticatedContext, resources);

        // Then
        assertEquals(2, result.size());
        assertEquals(ResourceStatus.CREATED, result.getFirst().getStatus());
        assertEquals(publicIpResource, result.getFirst().getCloudResource());
        assertEquals(ResourceStatus.UPDATED, result.get(1).getStatus());

        // Verify SKU attributes were not set due to null SKU
        SkuAttributes skuAttributes = publicIpResource.getTypedAttributes(SkuAttributes.class, SkuAttributes::new);
        assertNull(skuAttributes.getSku());
        assertNull(skuAttributes.getIpAllocationMethod());

        verify(azureClient, times(1)).getPublicIpAddresses(anySet(), anyString());
        verify(azureOutboundManager, times(1)).updateNetworkOutbound(network, PUBLIC_IP);
    }

    @Test
    void testSyncWhenPublicIpResourceNotFoundInCloudResources() {
        // Given
        String publicIpId = "/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/publicIPAddresses/publicIp1";
        String differentPublicIpId = "/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/publicIPAddresses/publicIp2";
        CloudResource publicIpResource = createCloudResourceWithReference("publicIp1", AZURE_PUBLIC_IP, publicIpId);
        CloudResource network = createCloudResource("network1", AZURE_NETWORK);
        List<CloudResource> resources = List.of(publicIpResource, network);

        when(azureClient.getPublicIpAddresses(anySet(), anyString())).thenReturn(List.of(publicIpAddress));
        when(publicIpAddress.id()).thenReturn(differentPublicIpId);
        when(azureOutboundManager.updateNetworkOutbound(network, PUBLIC_IP))
                .thenReturn(new CloudResourceStatus(network, ResourceStatus.UPDATED));

        // When
        List<CloudResourceStatus> result = underTest.sync(authenticatedContext, resources);

        // Then
        assertEquals(1, result.size());
        assertEquals(ResourceStatus.UPDATED, result.getFirst().getStatus());
        assertEquals(network, result.getFirst().getCloudResource());

        verify(azureClient, times(1)).getPublicIpAddresses(anySet(), anyString());
        verify(azureOutboundManager, times(1)).updateNetworkOutbound(network, PUBLIC_IP);
    }

    @Test
    void testSyncWhenNoPublicIpResourcesExist() {
        // Given
        CloudResource network = createCloudResource("network1", AZURE_NETWORK);
        List<CloudResource> resources = List.of(network);

        // When
        List<CloudResourceStatus> result = underTest.sync(authenticatedContext, resources);

        // Then
        assertTrue(result.isEmpty());
        verify(azureClient, never()).getPublicIpAddresses(anySet(), anyString());
        verify(azureOutboundManager, never()).updateNetworkOutbound(any(), any());
    }

    @Test
    void testSyncWhenPublicIpResourcesExistButNoValidReference() {
        // Given
        CloudResource publicIpResource = createCloudResourceWithReference("publicIp1", AZURE_PUBLIC_IP, "");
        CloudResource network = createCloudResource("network1", AZURE_NETWORK);
        List<CloudResource> resources = List.of(publicIpResource, network);

        // When
        List<CloudResourceStatus> result = underTest.sync(authenticatedContext, resources);

        // Then
        assertTrue(result.isEmpty());
        verify(azureClient, never()).getPublicIpAddresses(anySet(), anyString());
        verify(azureOutboundManager, never()).updateNetworkOutbound(any(), any());
    }

    @Test
    void testSyncWhenPublicIpResourcesExistButAllReferencesAreBlank() {
        // Given
        CloudResource publicIpResource1 = createCloudResourceWithReference("publicIp1", AZURE_PUBLIC_IP, "");
        CloudResource publicIpResource2 = createCloudResourceWithReference("publicIp2", AZURE_PUBLIC_IP, "   ");
        CloudResource network = createCloudResource("network1", AZURE_NETWORK);
        List<CloudResource> resources = List.of(publicIpResource1, publicIpResource2, network);

        // When
        List<CloudResourceStatus> result = underTest.sync(authenticatedContext, resources);

        // Then
        assertTrue(result.isEmpty());
        verify(azureClient, never()).getPublicIpAddresses(anySet(), anyString());
        verify(azureOutboundManager, never()).updateNetworkOutbound(any(), any());
    }

    @Test
    void testSyncWhenNoNetworkResourceExists() {
        // Given
        String publicIpId = "/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/publicIPAddresses/publicIp1";
        CloudResource publicIpResource = createCloudResourceWithReference("publicIp1", AZURE_PUBLIC_IP, publicIpId);
        List<CloudResource> resources = List.of(publicIpResource);

        when(azureClient.getPublicIpAddresses(anySet(), anyString())).thenReturn(List.of(publicIpAddress));
        when(publicIpAddress.id()).thenReturn(publicIpId);
        when(publicIpAddress.sku()).thenReturn(publicIPSkuType);
        when(publicIPSkuType.sku()).thenReturn(publicIpAddressSku);
        when(publicIpAddressSku.name()).thenReturn(PublicIpAddressSkuName.STANDARD);
        when(publicIpAddress.ipAllocationMethod()).thenReturn(STATIC);

        // When
        List<CloudResourceStatus> result = underTest.sync(authenticatedContext, resources);

        // Then
        assertEquals(1, result.size());
        assertEquals(ResourceStatus.CREATED, result.get(0).getStatus());
        assertEquals(publicIpResource, result.get(0).getCloudResource());

        verify(azureClient, times(1)).getPublicIpAddresses(anySet(), anyString());
        verify(azureOutboundManager, never()).updateNetworkOutbound(any(), any());
    }

    @Test
    void testSyncWhenMultiplePublicIpResourcesExist() {
        // Given
        String publicIpId1 = "/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/publicIPAddresses/publicIp1";
        String publicIpId2 = "/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/publicIPAddresses/publicIp2";
        CloudResource publicIpResource1 = createCloudResourceWithReference("publicIp1", AZURE_PUBLIC_IP, publicIpId1);
        CloudResource publicIpResource2 = createCloudResourceWithReference("publicIp2", AZURE_PUBLIC_IP, publicIpId2);
        CloudResource network = createCloudResource("network1", AZURE_NETWORK);
        List<CloudResource> resources = List.of(publicIpResource1, publicIpResource2, network);

        PublicIpAddress publicIpAddress2 = org.mockito.Mockito.mock(PublicIpAddress.class);
        PublicIPSkuType publicIPSkuType2 = org.mockito.Mockito.mock(PublicIPSkuType.class);
        PublicIpAddressSku publicIpAddressSku2 = org.mockito.Mockito.mock(PublicIpAddressSku.class);

        when(azureClient.getPublicIpAddresses(anySet(), anyString())).thenReturn(List.of(publicIpAddress, publicIpAddress2));

        // First public IP
        when(publicIpAddress.id()).thenReturn(publicIpId1);
        when(publicIpAddress.sku()).thenReturn(publicIPSkuType);
        when(publicIPSkuType.sku()).thenReturn(publicIpAddressSku);
        when(publicIpAddressSku.name()).thenReturn(PublicIpAddressSkuName.STANDARD);
        when(publicIpAddress.ipAllocationMethod()).thenReturn(STATIC);

        // Second public IP
        when(publicIpAddress2.id()).thenReturn(publicIpId2);
        when(publicIpAddress2.sku()).thenReturn(publicIPSkuType2);
        when(publicIPSkuType2.sku()).thenReturn(publicIpAddressSku2);
        when(publicIpAddressSku2.name()).thenReturn(PublicIpAddressSkuName.BASIC);
        when(publicIpAddress2.ipAllocationMethod()).thenReturn(DYNAMIC);

        when(azureOutboundManager.updateNetworkOutbound(network, PUBLIC_IP))
                .thenReturn(new CloudResourceStatus(network, ResourceStatus.UPDATED));

        // When
        List<CloudResourceStatus> result = underTest.sync(authenticatedContext, resources);

        // Then
        assertEquals(3, result.size());

        // Verify first public IP
        CloudResourceStatus firstResult = result.stream()
                .filter(r -> r.getCloudResource().equals(publicIpResource1))
                .findFirst()
                .orElseThrow();
        assertEquals(ResourceStatus.CREATED, firstResult.getStatus());
        SkuAttributes skuAttributes1 = publicIpResource1.getTypedAttributes(SkuAttributes.class, SkuAttributes::new);
        assertEquals("Standard", skuAttributes1.getSku());
        assertEquals("Static", skuAttributes1.getIpAllocationMethod());

        // Verify second public IP
        CloudResourceStatus secondResult = result.stream()
                .filter(r -> r.getCloudResource().equals(publicIpResource2))
                .findFirst()
                .orElseThrow();
        assertEquals(ResourceStatus.CREATED, secondResult.getStatus());
        SkuAttributes skuAttributes2 = publicIpResource2.getTypedAttributes(SkuAttributes.class, SkuAttributes::new);
        assertEquals("Basic", skuAttributes2.getSku());
        assertEquals("Dynamic", skuAttributes2.getIpAllocationMethod());

        // Verify network update
        CloudResourceStatus networkResult = result.stream()
                .filter(r -> r.getCloudResource().equals(network))
                .findFirst()
                .orElseThrow();
        assertEquals(ResourceStatus.UPDATED, networkResult.getStatus());

        verify(azureClient, times(1)).getPublicIpAddresses(anySet(), anyString());
        verify(azureOutboundManager, times(1)).updateNetworkOutbound(network, PUBLIC_IP);
    }

    @Test
    void testSyncWhenMixedPublicIpResourcesWithSomeBasicSku() {
        // Given
        CloudResource publicIpBasic = createPublicIpResource("publicIp1", "basic");
        CloudResource publicIpStandard = createPublicIpResource("publicIp2", "standard");
        List<CloudResource> resources = List.of(publicIpBasic, publicIpStandard);

        // When
        boolean result = underTest.shouldSync(authenticatedContext, resources);

        // Then
        assertTrue(result);
        verify(azureOutboundManager, never()).shouldSyncForOutbound(anyList());
    }

    private CloudResource createCloudResource(String name, ResourceType type) {
        return CloudResource.builder()
                .withName(name)
                .withType(type)
                .withStatus(CommonStatus.CREATED)
                .build();
    }

    private CloudResource createCloudResourceWithReference(String name, ResourceType type, String reference) {
        return CloudResource.builder()
                .withName(name)
                .withType(type)
                .withStatus(CommonStatus.CREATED)
                .withReference(reference)
                .build();
    }

    private CloudResource createPublicIpResource(String name, String skuValue) {
        CloudResource resource = createCloudResource(name, AZURE_PUBLIC_IP);
        if (skuValue != null) {
            SkuAttributes skuAttributes = new SkuAttributes();
            skuAttributes.setSku(skuValue);
            resource.setTypedAttributes(skuAttributes);
        }
        return resource;
    }
}