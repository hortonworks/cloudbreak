package com.sequenceiq.cloudbreak.cloud.azure.providersync;

import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_LOAD_BALANCER;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_NETWORK;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_NETWORK_INTERFACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.network.models.LoadBalancer;
import com.azure.resourcemanager.network.models.LoadBalancerSkuType;
import com.sequenceiq.cloudbreak.cloud.azure.AzureConstants;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.OutboundType;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.SkuAttributes;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AzureLoadBalancerSyncerTest {

    private static final String LB_RESOURCE_ID =
            "/subscriptions/mySubscription/resourceGroups/resourceGroupName/providers/Microsoft.Network/loadBalancers/lb1";

    @Mock
    private NetworkInterfaceLoadBalancerChecker networkInterfaceLoadBalancerChecker;

    @Mock
    private AzureOutboundManager azureOutboundManager;

    @InjectMocks
    private AzureLoadBalancerSyncer underTest;

    @Test
    void syncShouldReturnCloudResourceStatuses() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
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

    @Test
    void testPlatform() {
        assertEquals(AzureConstants.PLATFORM, underTest.platform());
    }

    @Test
    void testVariant() {
        assertEquals(AzureConstants.VARIANT, underTest.variant());
    }

    @Test
    void shouldSyncWhenNoLoadBalancersAndOutboundSyncRequired() {
        List<CloudResource> resources = List.of(createCloudResource("network", AZURE_NETWORK));
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        when(azureOutboundManager.shouldSyncForOutbound(resources)).thenReturn(true);

        boolean shouldSync = underTest.shouldSync(authenticatedContext, resources);

        assertTrue(shouldSync);
    }

    @Test
    void shouldSyncWhenLoadBalancerHasBasicSku() {
        CloudResource lb = createCloudResource("lb", AZURE_LOAD_BALANCER);
        SkuAttributes skuAttributes = new SkuAttributes();
        skuAttributes.setSku("Basic");
        lb.setTypedAttributes(skuAttributes);
        List<CloudResource> resources = List.of(lb);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        when(azureOutboundManager.shouldSyncForOutbound(resources)).thenReturn(false);

        boolean shouldSync = underTest.shouldSync(authenticatedContext, resources);

        assertTrue(shouldSync);
    }

    @Test
    void shouldNotSyncWhenLoadBalancerHasStandardSkuAndNoOutboundSync() {
        CloudResource lb = createCloudResource("lb", AZURE_LOAD_BALANCER);
        SkuAttributes skuAttributes = new SkuAttributes();
        skuAttributes.setSku("Standard");
        lb.setTypedAttributes(skuAttributes);
        List<CloudResource> resources = List.of(lb);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        when(azureOutboundManager.shouldSyncForOutbound(resources)).thenReturn(false);

        boolean shouldSync = underTest.shouldSync(authenticatedContext, resources);

        assertFalse(shouldSync);
    }

    @Test
    void syncShouldProcessNetworkInterfacesWhenNoLoadBalancers() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        CloudResource network = createCloudResource("network", AZURE_NETWORK);
        CloudResource nic = createNetworkInterface();
        List<CloudResource> resources = List.of(network, nic);

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);

        NetworkInterfaceCheckResult checkResult = new NetworkInterfaceCheckResult("Found LB", Map.of(), Set.of());
        when(networkInterfaceLoadBalancerChecker.checkNetworkInterfacesWithCommonLoadBalancer(
                List.of(nic.getReference()), azureClient)).thenReturn(checkResult);

        List<CloudResourceStatus> statuses = underTest.sync(authenticatedContext, resources);

        assertNotNull(statuses);
        assertEquals(0, statuses.size());
    }

    @Test
    void syncShouldProcessNetworkInterfacesWithCommonLoadBalancer() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        CloudResource network = createCloudResource("network", AZURE_NETWORK);
        CloudResource nic = createNetworkInterface();
        LoadBalancer commonLb = mock(LoadBalancer.class);
        List<CloudResource> resources = List.of(network, nic);

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(commonLb.id()).thenReturn(LB_RESOURCE_ID);
        when(commonLb.name()).thenReturn("lb1");
        when(commonLb.resourceGroupName()).thenReturn("resourceGroupName");
        when(commonLb.sku()).thenReturn(LoadBalancerSkuType.STANDARD);

        NetworkInterfaceCheckResult checkResult = new NetworkInterfaceCheckResult(
            "Found common LB",
            Map.of(),
            Set.of(commonLb)
        );
        when(networkInterfaceLoadBalancerChecker.checkNetworkInterfacesWithCommonLoadBalancer(
                List.of(nic.getReference()), azureClient)).thenReturn(checkResult);
        when(azureOutboundManager.updateNetworkOutbound(network, OutboundType.LOAD_BALANCER))
            .thenReturn(new CloudResourceStatus(network, ResourceStatus.UPDATED));

        List<CloudResourceStatus> statuses = underTest.sync(authenticatedContext, resources);

        assertNotNull(statuses);
        // One for network update, one for load balancer
        assertEquals(2, statuses.size());
        assertTrue(statuses.stream().anyMatch(s -> s.getStatus() == ResourceStatus.UPDATED));
    }

    @Test
    void syncShouldSkipNetworkUpdateWhenNetworkResourceMissing() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        CloudResource nic = createNetworkInterface();
        LoadBalancer commonLb = mock(LoadBalancer.class);
        List<CloudResource> resources = List.of(nic);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);

        NetworkInterfaceCheckResult checkResult = new NetworkInterfaceCheckResult(
            "Found common LB",
            Map.of(),
            Set.of(commonLb)
        );
        when(networkInterfaceLoadBalancerChecker.checkNetworkInterfacesWithCommonLoadBalancer(
                List.of(nic.getReference()), azureClient)).thenReturn(checkResult);

        List<CloudResourceStatus> statuses = underTest.sync(authenticatedContext, resources);

        assertNotNull(statuses);
        assertEquals(0, statuses.size());
    }

    @Test
    void shouldSyncWhenBothOutboundSyncAndLoadBalancerSyncRequired() {
        CloudResource lb = createCloudResource("lb", AZURE_LOAD_BALANCER);
        SkuAttributes skuAttributes = new SkuAttributes();
        skuAttributes.setSku("Basic");
        lb.setTypedAttributes(skuAttributes);
        List<CloudResource> resources = List.of(lb);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        when(azureOutboundManager.shouldSyncForOutbound(resources)).thenReturn(true);

        boolean shouldSync = underTest.shouldSync(authenticatedContext, resources);

        assertTrue(shouldSync);
    }

    @Test
    void syncShouldUpdateExistingLoadBalancerResource() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        CloudResource existingLb = CloudResource.builder()
                .withName("lb1")
                .withType(AZURE_LOAD_BALANCER)
                .withStatus(CREATED)
                .withReference(LB_RESOURCE_ID)
                .withGroup("test")
                .build();
        LoadBalancer loadBalancer = mock(LoadBalancer.class);

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(loadBalancer.id()).thenReturn(LB_RESOURCE_ID);
        when(loadBalancer.sku()).thenReturn(LoadBalancerSkuType.STANDARD);
        when(azureClient.getLoadBalancers(Set.of(LB_RESOURCE_ID), "resourceGroupName")).thenReturn(List.of(loadBalancer));
        when(azureOutboundManager.shouldSyncForOutbound(List.of(existingLb))).thenReturn(false);

        List<CloudResourceStatus> statuses = underTest.sync(authenticatedContext, List.of(existingLb));

        assertNotNull(statuses);
        assertEquals(1, statuses.size());
        CloudResourceStatus status = statuses.getFirst();
        assertEquals(ResourceStatus.CREATED, status.getStatus());
        SkuAttributes skuAttributes = status.getCloudResource().getParameter(CloudResource.ATTRIBUTES, SkuAttributes.class);
        assertEquals("Standard", skuAttributes.getSku());
    }

    @Test
    void syncShouldCreateNewLoadBalancerResourceWhenNotFound() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        LoadBalancer discoveredLb = mock(LoadBalancer.class);
        String newLbId = "/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/loadBalancers/newlb";

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(discoveredLb.id()).thenReturn(newLbId);
        when(discoveredLb.name()).thenReturn("newlb");
        when(discoveredLb.resourceGroupName()).thenReturn("rg1");
        when(discoveredLb.sku()).thenReturn(LoadBalancerSkuType.STANDARD);

        CloudResource network = createCloudResource("network", AZURE_NETWORK);
        CloudResource nic = createNetworkInterface();
        List<CloudResource> resources = List.of(network, nic);

        NetworkInterfaceCheckResult checkResult = new NetworkInterfaceCheckResult(
            "Found new LB",
            Map.of(),
            Set.of(discoveredLb)
        );
        when(networkInterfaceLoadBalancerChecker.checkNetworkInterfacesWithCommonLoadBalancer(
                List.of(nic.getReference()), azureClient)).thenReturn(checkResult);
        when(azureOutboundManager.updateNetworkOutbound(network, OutboundType.LOAD_BALANCER))
            .thenReturn(new CloudResourceStatus(network, ResourceStatus.UPDATED));

        List<CloudResourceStatus> statuses = underTest.sync(authenticatedContext, resources);

        assertNotNull(statuses);
        assertEquals(2, statuses.size());
        assertEquals(newLbId, statuses.get(1).getCloudResource().getReference());
    }

    @Test
    void shouldUpdateExistingCloudResourceStatusInResultList() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        CloudResource lb1 = CloudResource.builder()
                .withType(AZURE_LOAD_BALANCER)
                .withStatus(CREATED)
                .withName("lb1")
                .withReference(LB_RESOURCE_ID)
                .withGroup("resourceGroupName")
                .build();
        LoadBalancer loadBalancer = mock(LoadBalancer.class);
        CloudResource network = createCloudResource("network", AZURE_NETWORK);
        CloudResource nic = createNetworkInterface();
        List<CloudResource> resources = List.of(lb1, network, nic);
        LoadBalancer commonLb = mock(LoadBalancer.class);

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(commonLb.id()).thenReturn(LB_RESOURCE_ID);
        when(commonLb.sku()).thenReturn(LoadBalancerSkuType.STANDARD);
        NetworkInterfaceCheckResult checkResult = new NetworkInterfaceCheckResult(
                "Found common LB",
                Map.of(),
                Set.of(commonLb)
        );
        when(networkInterfaceLoadBalancerChecker.checkNetworkInterfacesWithCommonLoadBalancer(
                List.of(nic.getReference()), azureClient)).thenReturn(checkResult);

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(loadBalancer.id()).thenReturn(LB_RESOURCE_ID);
        when(loadBalancer.sku()).thenReturn(LoadBalancerSkuType.STANDARD);
        when(azureClient.getLoadBalancers(Set.of(LB_RESOURCE_ID), "resourceGroupName")).thenReturn(List.of(loadBalancer));
        when(azureOutboundManager.shouldSyncForOutbound(resources)).thenReturn(true);
        when(azureOutboundManager.updateNetworkOutbound(network, OutboundType.LOAD_BALANCER))
                .thenReturn(new CloudResourceStatus(network, ResourceStatus.UPDATED));

        List<CloudResourceStatus> statuses = underTest.sync(authenticatedContext, resources);
        assertNotNull(statuses);
        assertEquals(2, statuses.size());
    }

    @Test
    void shouldHandleEmptyLoadBalancerListWhenFiltered() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        CloudResource lb1 = CloudResource.builder()
                .withType(AZURE_LOAD_BALANCER)
                .withStatus(CREATED)
                .withName("lb1")
                // Simulating an empty reference to test filtering logic
                .withReference(" ")
                .withGroup("resourceGroupName")
                .build();
        List<CloudResource> resources = List.of(lb1);

        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureOutboundManager.shouldSyncForOutbound(resources)).thenReturn(false);

        List<CloudResourceStatus> statuses = underTest.sync(authenticatedContext, resources);

        assertNotNull(statuses);
        assertEquals(0, statuses.size());
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

    private CloudResource createNetworkInterface() {
        return CloudResource.builder()
                .withName("nic")
                .withType(AZURE_NETWORK_INTERFACE)
                .withStatus(CREATED)
                .withReference("/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/networkInterfaces/nic1")
                .withGroup("test")
                .build();
    }
}
