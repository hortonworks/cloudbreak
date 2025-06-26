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

import com.azure.resourcemanager.network.models.LoadBalancer;
import com.azure.resourcemanager.network.models.LoadBalancerSkuType;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.SkuAttributes;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AzureLoadBalancerSyncerTest {

    private static final String LB_RESOURCE_ID =
            "/subscriptions/mySubscription/resourceGroups/resourceGroupName/providers/Microsoft.Network/loadBalancers/lb1";

    @InjectMocks
    AzureLoadBalancerSyncer underTest;

    @Test
    void syncShouldReturnCloudResourceStatuses() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        CloudResource instance1 = createCloudResource("i1", AZURE_INSTANCE);
        CloudResource ip1 = createCloudResource("ip1", AZURE_PUBLIC_IP);
        CloudResource lb1 = createCloudResource("lb1", AZURE_LOAD_BALANCER);
        CloudResource lb2 = CloudResource.builder()
                .cloudResource(lb1)
                .withReference(LB_RESOURCE_ID)
                .build();
        LoadBalancer loadBalancer = mock(LoadBalancer.class);

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(loadBalancer.id()).thenReturn(LB_RESOURCE_ID);
        when(loadBalancer.sku()).thenReturn(LoadBalancerSkuType.BASIC);
        when(azureClient.getLoadBalancers(Set.of(LB_RESOURCE_ID), "resourceGroupName")).thenReturn(List.of(loadBalancer));

        List<CloudResourceStatus> cloudResourceStatuses = underTest.sync(authenticatedContext, List.of(lb2));

        assertNotNull(cloudResourceStatuses);
        assertEquals(1, cloudResourceStatuses.size());
        CloudResourceStatus status = cloudResourceStatuses.getFirst();
        assertEquals(ResourceStatus.CREATED, status.getStatus());
        CloudResource actualResource = status.getCloudResource();
        assertEquals(LB_RESOURCE_ID, actualResource.getReference());
        SkuAttributes skuAttributes = actualResource.getParameter(CloudResource.ATTRIBUTES, SkuAttributes.class);
        assertNotNull(skuAttributes);
        assertEquals("Basic", skuAttributes.getSku());
    }

    @Test
    void syncShouldNotAddCloudResourceWhenResourceNotFound() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        CloudResource lb1 = createCloudResource("lb1", AZURE_LOAD_BALANCER);
        CloudResource lb2 = CloudResource.builder()
                .cloudResource(lb1)
                .withReference(LB_RESOURCE_ID)
                .build();

        LoadBalancer loadBalancer = mock(LoadBalancer.class);

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(loadBalancer.id()).thenReturn("anotherId");
        when(azureClient.getLoadBalancers(Set.of(LB_RESOURCE_ID), "resourceGroupName")).thenReturn(List.of(loadBalancer));

        List<CloudResourceStatus> cloudResourceStatuses = underTest.sync(authenticatedContext, List.of(lb2));
        assertEquals(0, cloudResourceStatuses.size());
    }

    @Test
    void syncShouldReturnEmptyListWhenLoadBalancerListIsEmpty() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        List<CloudResource> resources = List.of();

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);

        List<CloudResourceStatus> cloudResourceStatuses = underTest.sync(authenticatedContext, resources);

        assertNotNull(cloudResourceStatuses);
        assertEquals(0, cloudResourceStatuses.size());
    }

    @Test
    void syncShouldSkipBlankLoadBalancerResourceId() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        CloudResource cloudResource = mock(CloudResource.class);
        List<CloudResource> resources = List.of(cloudResource);

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(cloudResource.getReference()).thenReturn(" ");
        when(cloudResource.getType()).thenReturn(AZURE_LOAD_BALANCER);

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