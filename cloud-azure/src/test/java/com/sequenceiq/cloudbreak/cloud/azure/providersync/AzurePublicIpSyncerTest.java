package com.sequenceiq.cloudbreak.cloud.azure.providersync;


import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_INSTANCE;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_LOAD_BALANCER;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_PUBLIC_IP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.network.models.IpAllocationMethod;
import com.azure.resourcemanager.network.models.PublicIPSkuType;
import com.azure.resourcemanager.network.models.PublicIpAddress;
import com.azure.resourcemanager.network.models.PublicIpAddressSku;
import com.azure.resourcemanager.network.models.PublicIpAddressSkuName;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.SkuAttributes;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AzurePublicIpSyncerTest {

    private static final String IP_RESOURCE_ID =
            "/subscriptions/mySubscription/resourceGroups/resourceGroupName/providers/Microsoft.Network/publicIPAddresses/ip1";

    @InjectMocks
    AzurePublicIpSyncer underTest;

    @Test
    void syncShouldReturnCloudResourceStatuses() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        CloudResource instance1 = createCloudResource("instance1", AZURE_INSTANCE);
        CloudResource lb1 = createCloudResource("lb1", AZURE_LOAD_BALANCER);
        CloudResource instance2 = createCloudResource("instance2", AZURE_INSTANCE);
        CloudResource ip1 = createCloudResource("ip1", AZURE_PUBLIC_IP);
        CloudResource ip2 = CloudResource.builder()
                .cloudResource(ip1)
                .withReference(IP_RESOURCE_ID)
                .build();

        PublicIpAddress publicIpAddress = mock(PublicIpAddress.class);
        PublicIPSkuType publicIpAddressSkuType = mock(PublicIPSkuType.class);
        PublicIpAddressSku publicIpAddressSku = mock(PublicIpAddressSku.class);

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(publicIpAddress.id()).thenReturn(IP_RESOURCE_ID);
        when(publicIpAddress.sku()).thenReturn(publicIpAddressSkuType);
        when(publicIpAddressSkuType.sku()).thenReturn(publicIpAddressSku);
        when(publicIpAddressSku.name()).thenReturn(PublicIpAddressSkuName.BASIC);
        when(publicIpAddress.ipAllocationMethod()).thenReturn(IpAllocationMethod.DYNAMIC);
        when(azureClient.getPublicIpAddresses(Set.of(IP_RESOURCE_ID), "resourceGroupName")).thenReturn(List.of(publicIpAddress));

        List<CloudResourceStatus> cloudResourceStatuses = underTest.sync(authenticatedContext, List.of(instance1, lb1, instance2, ip2));

        assertNotNull(cloudResourceStatuses);
        assertEquals(1, cloudResourceStatuses.size());
        CloudResourceStatus status = cloudResourceStatuses.getFirst();
        assertEquals(ResourceStatus.CREATED, status.getStatus());
        CloudResource actualResource = status.getCloudResource();
        assertEquals(IP_RESOURCE_ID, actualResource.getReference());
        SkuAttributes skuAttributes = actualResource.getParameter(CloudResource.ATTRIBUTES, SkuAttributes.class);
        assertNotNull(skuAttributes);
        assertEquals("Basic", skuAttributes.getSku());
        assertEquals("Dynamic", skuAttributes.getIpAllocationMethod());
    }

    @Test
    void syncShouldNotAddCloudResourceWhenResourceNotFound() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        CloudResource instance1 = createCloudResource("instance1", AZURE_INSTANCE);
        CloudResource lb1 = createCloudResource("lb1", AZURE_LOAD_BALANCER);
        CloudResource instance2 = createCloudResource("instance2", AZURE_INSTANCE);
        CloudResource ip1 = createCloudResource("ip1", AZURE_PUBLIC_IP);
        CloudResource ip2 = CloudResource.builder()
                .cloudResource(ip1)
                .withReference(IP_RESOURCE_ID)
                .build();

        PublicIpAddress publicIpAddress = mock(PublicIpAddress.class);

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(publicIpAddress.id()).thenReturn("anotherId");
        when(azureClient.getPublicIpAddresses(Set.of(IP_RESOURCE_ID), "resourceGroupName")).thenReturn(List.of(publicIpAddress));

        List<CloudResourceStatus> cloudResourceStatuses = underTest.sync(authenticatedContext, List.of(instance1, lb1, instance2, ip2));
        assertEquals(0, cloudResourceStatuses.size());
    }

    @Test
    void syncShouldReturnEmptyListWhenPublicIpListIsEmpty() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        CloudResource instance1 = createCloudResource("instance1", AZURE_INSTANCE);
        CloudResource lb1 = createCloudResource("lb1", AZURE_LOAD_BALANCER);
        List<CloudResource> resources = List.of(instance1, lb1);

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);

        List<CloudResourceStatus> cloudResourceStatuses = underTest.sync(authenticatedContext, resources);

        assertNotNull(cloudResourceStatuses);
        assertEquals(0, cloudResourceStatuses.size());
    }

    @Test
    void syncShouldSkipBlankPublicIpResourceId() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        CloudResource cloudResource = mock(CloudResource.class);
        List<CloudResource> resources = List.of(cloudResource);

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(cloudResource.getReference()).thenReturn(" ");
        when(cloudResource.getType()).thenReturn(AZURE_PUBLIC_IP);

        List<CloudResourceStatus> cloudResourceStatuses = underTest.sync(authenticatedContext, resources);

        assertNotNull(cloudResourceStatuses);
        assertEquals(0, cloudResourceStatuses.size());
    }

    private CloudResource createCloudResource(String name, ResourceType resourceType) {
        return CloudResource.builder()
                .withName(name)
                .withStatus(CREATED)
                .withType(resourceType)
                .withInstanceId("instanceId")
                .withGroup("test")
                .build();
    }

}