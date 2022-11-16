package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_PRIVATE_DNS_ZONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.resources.Subscription;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureResourceIdProviderService;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@ExtendWith(MockitoExtension.class)
public class AzureDnsZoneServiceTest {

    private static final Long STACK_ID = 12L;

    private static final String RESOURCE_GROUP = "resource-group";

    private static final String NETWORK_ID = "networkId";

    private static final String NETWORK_RG = "networkRg";

    private static final String DEPLOYMENT_ID =
            "/subscriptions/subscription-id/resourceGroups/resourcegroup/providers/Microsoft.Resources/deployments/deployment-id";

    private static final String SUBSCRIPTION_ID = "subscription-id";

    @Mock
    private AzureResourceDeploymentHelperService azureResourceDeploymentHelperService;

    @Mock
    private AzureResourceIdProviderService azureResourceIdProviderService;

    @Mock
    private AzureResourcePersistenceHelperService azureResourcePersistenceHelperService;

    @Mock
    private AzurePrivateEndpointServicesProvider azurePrivateEndpointServicesProvider;

    @InjectMocks
    private AzureDnsZoneService underTest;

    private AuthenticatedContext ac;

    @Mock
    private AzureClient client;

    @BeforeEach
    public void setUp() {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(STACK_ID)
                .withName("")
                .withCrn("")
                .withPlatform("")
                .build();
        CloudCredential cloudCredential = new CloudCredential(STACK_ID.toString(), "", "account");
        ac = new AuthenticatedContext(cloudContext, cloudCredential);

        when(azurePrivateEndpointServicesProvider.getCdpManagedDnsZones(any()))
                .thenReturn(List.of(AzurePrivateDnsZoneServiceEnum.STORAGE, AzurePrivateDnsZoneServiceEnum.POSTGRES));
    }

    @Test
    public void testCheckOrCreateWhenAllResourceExists() {

        when(client.checkIfDnsZonesDeployed(any(), any())).thenReturn(true);

        underTest.checkOrCreateDnsZones(ac, client, getNetworkView(), RESOURCE_GROUP, Collections.emptyMap(), Set.of());

        verify(azureResourcePersistenceHelperService, times(0)).persistCloudResource(any(), any(), any(), any());
        verify(azureResourcePersistenceHelperService, times(0)).updateCloudResource(any(), any(), any(), any(), any());
        verify(azureResourcePersistenceHelperService, times(0)).isRequested(any(), any());
        verify(azureResourcePersistenceHelperService, times(0)).isCreated(any(), any());
        verify(azureResourceDeploymentHelperService, times(0)).pollForCreation(any(), any());

    }

    @Test
    public void testCheckOrCreateWhenDnsZoneResourceNotExistsButRequested() {
        when(azureResourceIdProviderService.generateDeploymentId(any(), any(), any())).thenReturn(DEPLOYMENT_ID);
        when(azureResourceDeploymentHelperService.getAzureNetwork(any(), any(), any())).thenReturn(mock(Network.class));
        when(client.getCurrentSubscription()).thenReturn(mock(Subscription.class));
        when(client.getCurrentSubscription().subscriptionId()).thenReturn(SUBSCRIPTION_ID);
        when(client.checkIfDnsZonesDeployed(any(), any())).thenReturn(false);
        when(azureResourcePersistenceHelperService.isRequested(DEPLOYMENT_ID, AZURE_PRIVATE_DNS_ZONE)).thenReturn(true);

        underTest.checkOrCreateDnsZones(ac, client, getNetworkView(), RESOURCE_GROUP, Collections.emptyMap(), Set.of());

        verify(azureResourcePersistenceHelperService, times(0)).persistCloudResource(any(), any(), any(), any());
        verify(azureResourcePersistenceHelperService, times(0)).updateCloudResource(any(), any(), any(), any(), any());
        verify(azureResourcePersistenceHelperService, times(1)).isRequested(any(), any());
        verify(azureResourcePersistenceHelperService, times(0)).isCreated(any(), any());
        verify(azureResourceDeploymentHelperService, times(1)).pollForCreation(any(), any());
    }

    @Test
    public void testCheckOrCreateWhenDnsZoneResourceNotExistsAndNotRequestedButAlreadyCreatedInDatabase() {
        when(azureResourceIdProviderService.generateDeploymentId(any(), any(), any())).thenReturn(DEPLOYMENT_ID);
        when(azureResourceDeploymentHelperService.getAzureNetwork(any(), any(), any())).thenReturn(mock(Network.class));
        when(client.getCurrentSubscription()).thenReturn(mock(Subscription.class));
        when(client.getCurrentSubscription().subscriptionId()).thenReturn(SUBSCRIPTION_ID);
        when(client.checkIfDnsZonesDeployed(any(), any())).thenReturn(false);
        when(azureResourcePersistenceHelperService.isRequested(DEPLOYMENT_ID, AZURE_PRIVATE_DNS_ZONE)).thenReturn(false);
        when(azureResourcePersistenceHelperService.isCreated(DEPLOYMENT_ID, AZURE_PRIVATE_DNS_ZONE)).thenReturn(true);

        underTest.checkOrCreateDnsZones(ac, client, getNetworkView(), RESOURCE_GROUP, Collections.emptyMap(), Set.of());

        verify(azureResourcePersistenceHelperService, times(0)).persistCloudResource(any(), any(), any(), any());
        verify(azureResourcePersistenceHelperService, times(2)).updateCloudResource(any(), any(), any(), any(), any());
        verify(azureResourcePersistenceHelperService, times(1)).isRequested(any(), any());
        verify(azureResourcePersistenceHelperService, times(1)).isCreated(any(), any());
        verify(azureResourceDeploymentHelperService, times(0)).pollForCreation(any(), any());
    }

    @Test
    public void testCheckOrCreateWhenDnsZoneResourceNotExistsAndNotRequestedAndNotCreatedInDatabase() {
        when(azureResourceIdProviderService.generateDeploymentId(any(), any(), any())).thenReturn(DEPLOYMENT_ID);
        when(azureResourceDeploymentHelperService.getAzureNetwork(any(), any(), any())).thenReturn(mock(Network.class));
        when(client.getCurrentSubscription()).thenReturn(mock(Subscription.class));
        when(client.getCurrentSubscription().subscriptionId()).thenReturn(SUBSCRIPTION_ID);
        when(client.checkIfDnsZonesDeployed(any(), any())).thenReturn(false);
        when(azureResourcePersistenceHelperService.isRequested(DEPLOYMENT_ID, AZURE_PRIVATE_DNS_ZONE)).thenReturn(false);
        when(azureResourcePersistenceHelperService.isCreated(DEPLOYMENT_ID, AZURE_PRIVATE_DNS_ZONE)).thenReturn(false);

        underTest.checkOrCreateDnsZones(ac, client, getNetworkView(), RESOURCE_GROUP, Collections.emptyMap(), Set.of());

        verify(azureResourcePersistenceHelperService, times(1)).persistCloudResource(any(), any(), any(), any());
        verify(azureResourcePersistenceHelperService, times(1)).updateCloudResource(any(), any(), any(), any(), any());
        verify(azureResourcePersistenceHelperService, times(1)).isRequested(any(), any());
        verify(azureResourcePersistenceHelperService, times(1)).isCreated(any(), any());
        verify(azureResourceDeploymentHelperService, times(0)).pollForCreation(any(), any());
    }

    @Test
    public void testCheckOrCreateWhenDnsZoneResourceNotExistsAndNotRequestedAndNotCreatedInDatabaseAndError() {
        when(azureResourceIdProviderService.generateDeploymentId(any(), any(), any())).thenReturn(DEPLOYMENT_ID);
        when(azureResourceDeploymentHelperService.getAzureNetwork(any(), any(), any())).thenReturn(mock(Network.class));
        when(client.getCurrentSubscription()).thenReturn(mock(Subscription.class));
        when(client.getCurrentSubscription().subscriptionId()).thenReturn(SUBSCRIPTION_ID);
        when(client.checkIfDnsZonesDeployed(any(), any())).thenReturn(false);
        when(azureResourcePersistenceHelperService.isRequested(DEPLOYMENT_ID, AZURE_PRIVATE_DNS_ZONE)).thenReturn(false);
        when(azureResourcePersistenceHelperService.isCreated(DEPLOYMENT_ID, AZURE_PRIVATE_DNS_ZONE)).thenReturn(false);
        doThrow(new CloudConnectorException("some message", null)).when(azureResourceDeploymentHelperService).deployTemplate(any(), any());

        CloudConnectorException exception = assertThrows(CloudConnectorException.class, () -> {
            underTest.checkOrCreateDnsZones(ac, client, getNetworkView(), RESOURCE_GROUP, Collections.emptyMap(), Set.of());
        });

        assertEquals("some message", exception.getMessage());

        verify(azureResourcePersistenceHelperService, times(1)).persistCloudResource(any(), any(), any(), any());
        verify(azureResourcePersistenceHelperService, times(0)).updateCloudResource(any(), any(), any(), any(), any());
        verify(azureResourcePersistenceHelperService, times(1)).isRequested(any(), any());
        verify(azureResourcePersistenceHelperService, times(1)).isCreated(any(), any());
        verify(azureResourceDeploymentHelperService, times(1)).pollForCreation(any(), any());
    }

    private AzureNetworkView getNetworkView() {
        AzureNetworkView networkView = new AzureNetworkView();
        networkView.setExistingNetwork(false);
        networkView.setNetworkId(NETWORK_ID);
        networkView.setResourceGroupName(NETWORK_RG);
        return networkView;
    }
}