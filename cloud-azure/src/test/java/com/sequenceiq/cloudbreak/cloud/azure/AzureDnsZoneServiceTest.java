package com.sequenceiq.cloudbreak.cloud.azure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.resources.Subscription;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureResourceIdProviderService;
import com.sequenceiq.cloudbreak.cloud.azure.task.dnszone.AzureDnsZoneCreationPoller;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceRetriever;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@RunWith(MockitoJUnitRunner.class)
public class AzureDnsZoneServiceTest {

    private static final Long STACK_ID = 12L;

    private static final String RESOURCE_GROUP = "resource-group";

    private static final String NETWORK_ID = "networkId";

    private static final String AZURE_NETWORK_ID =
            "/subscriptions/subscription-id/resourceGroups/networkRg/providers/Microsoft.Network/virtualNetworks/networkId";

    private static final String NETWORK_RG = "networkRg";

    private static final List<String> PRIVATE_ENDPOINT_SERVICE_NAME_LIST = List.of("postgresqlServer", "Blob");

    private static final String DEPLOYMENT_ID =
            "/subscriptions/subscription-id/resourceGroups/resourcegroup/providers/Microsoft.Resources/deployments/deployment-id";

    private static final String DEPLOYMENT_NAME = "postgresqlserver-blob-dns-zones";

    private static final String NETWORK_LINK_DEPLOYMENT_NAME = "postgresqlserver-blob-networkid-links";

    private static final String SUBSCRIPTION_ID = "subscription-id";

    @Mock
    private AzureNetworkDnsZoneTemplateBuilder azureNetworkDnsZoneTemplateBuilder;

    @Mock
    private PersistenceRetriever resourcePersistenceRetriever;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock
    private AzureDnsZoneCreationPoller azureDnsZoneCreationPoller;

    @Mock
    private AzureResourceIdProviderService azureResourceIdProviderService;

    @InjectMocks
    private AzureDnsZoneService underTest;

    private AuthenticatedContext ac;

    @Mock
    private AzureClient client;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(underTest, "privateEndpointServices", PRIVATE_ENDPOINT_SERVICE_NAME_LIST);
        CloudContext cloudContext = new CloudContext(STACK_ID, "", "", "", "");
        CloudCredential cloudCredential = new CloudCredential(STACK_ID.toString(), "");
        ac = new AuthenticatedContext(cloudContext, cloudCredential);

        when(azureResourceIdProviderService.generateDeploymentId(any(), any(), any())).thenReturn(DEPLOYMENT_ID);
        when(client.getCurrentSubscription()).thenReturn(mock(Subscription.class));
        when(client.getCurrentSubscription().subscriptionId()).thenReturn(SUBSCRIPTION_ID);
        when(client.getNetworkByResourceGroup(NETWORK_RG, NETWORK_ID)).thenReturn(mock(Network.class));
    }

    @Test
    public void testCheckOrCreateWhenAllResourceExists() {

        when(client.checkIfDnsZonesDeployed(any(), any())).thenReturn(true);
        when(client.checkIfNetworkLinksDeployed(any(), any(), any())).thenReturn(true);

        underTest.getOrCreateDnsZones(ac, client, getNetworkView(), RESOURCE_GROUP, Collections.emptyMap());

        verify(persistenceNotifier, times(0)).notifyAllocation(any(), any());
        verify(persistenceNotifier, times(0)).notifyUpdate(any(), any());
        verify(resourcePersistenceRetriever, times(0)).notifyRetrieve(any(), any(), any());
    }

    @Test
    public void testCheckOrCreateWhenDnsZoneResourceExists() {

        when(client.checkIfDnsZonesDeployed(any(), any())).thenReturn(true);
        when(client.checkIfNetworkLinksDeployed(any(), any(), any())).thenReturn(false);

        underTest.getOrCreateDnsZones(ac, client, getNetworkView(), RESOURCE_GROUP, Collections.emptyMap());

        verify(persistenceNotifier, times(0)).notifyAllocation(any(), any());
        verify(persistenceNotifier, times(0)).notifyUpdate(any(), any());
        verify(resourcePersistenceRetriever, times(0)).notifyRetrieve(any(), any(), any());
    }

    @Test
    public void testCheckOrCreateWhenDnsZoneIsAlreadyRequested() {

        Network network = mock(Network.class);

        when(client.getTemplateDeploymentCommonStatus(RESOURCE_GROUP, DEPLOYMENT_NAME)).thenReturn(CommonStatus.CREATED);
        when(client.checkIfDnsZonesDeployed(any(), any())).thenReturn(false);
        when(resourcePersistenceRetriever.notifyRetrieve(DEPLOYMENT_ID, CommonStatus.REQUESTED, ResourceType.AZURE_PRIVATE_DNS_ZONE))
                .thenReturn(Optional.of(buildCloudResource()));
        when(client.getNetworkByResourceGroup(any(), any())).thenReturn(network);
        when(network.id()).thenReturn(AZURE_NETWORK_ID);

        underTest.getOrCreateDnsZones(ac, client, getNetworkView(), RESOURCE_GROUP, Collections.emptyMap());
        verify(persistenceNotifier, times(0)).notifyAllocation(any(), any());
        verify(persistenceNotifier, times(1)).notifyUpdate(any(), any());
        verify(resourcePersistenceRetriever, times(1)).notifyRetrieve(any(), any(), any());

    }

    @Test
    public void testCheckOrCreateWhenDnsZoneIsNotRequested() {

        when(client.checkIfDnsZonesDeployed(any(), any())).thenReturn(false);
        when(resourcePersistenceRetriever.notifyRetrieve(DEPLOYMENT_ID, CommonStatus.REQUESTED, ResourceType.AZURE_PRIVATE_DNS_ZONE))
                .thenReturn(Optional.empty());

        underTest.getOrCreateDnsZones(ac, client, getNetworkView(), RESOURCE_GROUP, Collections.emptyMap());

        verify(persistenceNotifier, times(1)).notifyAllocation(any(), any());
        verify(persistenceNotifier, times(1)).notifyUpdate(any(), any());
        verify(resourcePersistenceRetriever, times(2)).notifyRetrieve(any(), any(), any());

    }

    private AzureNetworkView getNetworkView() {
        AzureNetworkView networkView = new AzureNetworkView();
        networkView.setExistingNetwork(false);
        networkView.setNetworkId(NETWORK_ID);
        networkView.setResourceGroupName(NETWORK_RG);
        return networkView;
    }

    private CloudResource buildCloudResource() {
        return CloudResource.builder()
                .name("dnsZone")
                .status(CommonStatus.REQUESTED)
                .persistent(true)
                .reference(AzureDnsZoneServiceTest.DEPLOYMENT_ID)
                .type(ResourceType.AZURE_PRIVATE_DNS_ZONE)
                .build();
    }
}