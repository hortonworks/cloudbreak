package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_VIRTUAL_NETWORK_LINK;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.resources.models.Subscription;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureResourceIdProviderService;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.network.PrivateDatabaseVariant;

@ExtendWith(MockitoExtension.class)
public class AzureNetworkLinkServiceTest {

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

    @Mock
    private AzureCloudResourceService azureCloudResourceService;

    @InjectMocks
    private AzureNetworkLinkService underTest;

    private AuthenticatedContext ac;

    @Mock
    private AzureClient client;

    @BeforeEach
    public void setUp() {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(STACK_ID)
                .withCrn("")
                .withPlatform("")
                .build();
        CloudCredential cloudCredential = new CloudCredential(STACK_ID.toString(), "", "account");
        ac = new AuthenticatedContext(cloudContext, cloudCredential);

        when(azurePrivateEndpointServicesProvider.getCdpManagedDnsZoneServices(any(), any(PrivateDatabaseVariant.class)))
                .thenReturn(List.of(AzureManagedPrivateDnsZoneServiceType.STORAGE, AzureManagedPrivateDnsZoneServiceType.POSTGRES));
    }

    @ParameterizedTest
    @EnumSource(value = PrivateDatabaseVariant.class, mode = Mode.INCLUDE,
            names = {"POSTGRES_WITH_NEW_DNS_ZONE", "FLEXIBLE_POSTGRES_WITH_DELEGATED_SUBNET_AND_NEW_DNS_ZONE"})
    public void testCheckOrCreateWhenNetworkLinkExists(PrivateDatabaseVariant variant) {
        when(client.checkIfNetworkLinksDeployed(any(), any(), any())).thenReturn(true);
        when(client.getCurrentSubscription()).thenReturn(mock(Subscription.class));
        when(client.getCurrentSubscription().subscriptionId()).thenReturn(SUBSCRIPTION_ID);

        underTest.checkOrCreateNetworkLinks(ac, client, getNetworkView(), RESOURCE_GROUP, Collections.emptyMap(), Set.of(), variant);

        verify(azureResourcePersistenceHelperService, times(0)).persistCloudResource(any(), any(), any(), any());
        verify(azureResourcePersistenceHelperService, times(0)).updateCloudResource(any(), any(), any(), any(), any());
        verify(azureResourcePersistenceHelperService, times(0)).isRequested(any(), any());
        verify(azureResourcePersistenceHelperService, times(0)).isCreated(any(), any());
        verify(azureResourceDeploymentHelperService, times(0)).pollForCreation(any(), any());

    }

    @ParameterizedTest
    @EnumSource(value = PrivateDatabaseVariant.class, mode = Mode.INCLUDE,
            names = {"POSTGRES_WITH_NEW_DNS_ZONE", "FLEXIBLE_POSTGRES_WITH_DELEGATED_SUBNET_AND_NEW_DNS_ZONE"})
    public void testCheckOrCreateWhenNetworkLinkNotExistsButRequested(PrivateDatabaseVariant variant) {
        when(azureResourceIdProviderService.generateDeploymentId(any(), any(), any())).thenReturn(DEPLOYMENT_ID);
        when(client.getCurrentSubscription()).thenReturn(mock(Subscription.class));
        when(client.getCurrentSubscription().subscriptionId()).thenReturn(SUBSCRIPTION_ID);
        when(client.checkIfNetworkLinksDeployed(any(), any(), any())).thenReturn(false);
        when(azureResourcePersistenceHelperService.isRequested(DEPLOYMENT_ID, AZURE_VIRTUAL_NETWORK_LINK)).thenReturn(true);

        underTest.checkOrCreateNetworkLinks(ac, client, getNetworkView(), RESOURCE_GROUP, Collections.emptyMap(), Set.of(), variant);

        verify(azureResourcePersistenceHelperService, times(0)).persistCloudResource(any(), any(), any(), any());
        verify(azureResourcePersistenceHelperService, times(0)).updateCloudResource(any(), any(), any(), any(), any());
        verify(azureResourcePersistenceHelperService, times(1)).isRequested(any(), any());
        verify(azureResourcePersistenceHelperService, times(0)).isCreated(any(), any());
        verify(azureResourceDeploymentHelperService, times(1)).pollForCreation(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = PrivateDatabaseVariant.class, mode = Mode.INCLUDE,
            names = {"POSTGRES_WITH_NEW_DNS_ZONE", "FLEXIBLE_POSTGRES_WITH_DELEGATED_SUBNET_AND_NEW_DNS_ZONE"})
    public void testCheckOrCreateWhenNetworkLinkNotExistsAndNotRequestedButAlreadyCreatedInDatabase(PrivateDatabaseVariant variant) {
        when(azureResourceIdProviderService.generateDeploymentId(any(), any(), any())).thenReturn(DEPLOYMENT_ID);
        when(azureResourceDeploymentHelperService.getAzureNetwork(any(), any(), any())).thenReturn(mock(Network.class));
        when(client.getCurrentSubscription()).thenReturn(mock(Subscription.class));
        when(client.getCurrentSubscription().subscriptionId()).thenReturn(SUBSCRIPTION_ID);
        when(client.checkIfNetworkLinksDeployed(any(), any(), any())).thenReturn(false);
        when(azureResourcePersistenceHelperService.isRequested(DEPLOYMENT_ID, AZURE_VIRTUAL_NETWORK_LINK)).thenReturn(false);
        when(azureResourcePersistenceHelperService.isCreated(DEPLOYMENT_ID, AZURE_VIRTUAL_NETWORK_LINK)).thenReturn(true);

        underTest.checkOrCreateNetworkLinks(ac, client, getNetworkView(), RESOURCE_GROUP, Collections.emptyMap(), Set.of(), variant);

        verify(azureResourcePersistenceHelperService, times(0)).persistCloudResource(any(), any(), any(), any());
        verify(azureResourcePersistenceHelperService, times(2)).updateCloudResource(any(), any(), any(), any(), any());
        verify(azureResourcePersistenceHelperService, times(1)).isRequested(any(), any());
        verify(azureResourcePersistenceHelperService, times(1)).isCreated(any(), any());
        verify(azureResourceDeploymentHelperService, times(0)).pollForCreation(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = PrivateDatabaseVariant.class, mode = Mode.INCLUDE,
            names = {"POSTGRES_WITH_NEW_DNS_ZONE", "FLEXIBLE_POSTGRES_WITH_DELEGATED_SUBNET_AND_NEW_DNS_ZONE"})
    public void testCheckOrCreateWhenNetworkLinkNotExistsAndNotRequestedAndNotCreatedInDatabase(PrivateDatabaseVariant variant) {
        when(azureResourceIdProviderService.generateDeploymentId(any(), any(), any())).thenReturn(DEPLOYMENT_ID);
        when(azureResourceDeploymentHelperService.getAzureNetwork(any(), any(), any())).thenReturn(mock(Network.class));
        when(client.getCurrentSubscription()).thenReturn(mock(Subscription.class));
        when(client.getCurrentSubscription().subscriptionId()).thenReturn(SUBSCRIPTION_ID);
        when(client.checkIfNetworkLinksDeployed(any(), any(), any())).thenReturn(false);
        when(azureResourcePersistenceHelperService.isRequested(DEPLOYMENT_ID, AZURE_VIRTUAL_NETWORK_LINK)).thenReturn(false);
        when(azureResourcePersistenceHelperService.isCreated(DEPLOYMENT_ID, AZURE_VIRTUAL_NETWORK_LINK)).thenReturn(false);

        underTest.checkOrCreateNetworkLinks(ac, client, getNetworkView(), RESOURCE_GROUP, Collections.emptyMap(), Set.of(), variant);

        verify(azureResourcePersistenceHelperService, times(1)).persistCloudResource(any(), any(), any(), any());
        verify(azureResourcePersistenceHelperService, times(1)).updateCloudResource(any(), any(), any(), any(), any());
        verify(azureResourcePersistenceHelperService, times(1)).isRequested(any(), any());
        verify(azureResourcePersistenceHelperService, times(1)).isCreated(any(), any());
        verify(azureResourceDeploymentHelperService, times(0)).pollForCreation(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = PrivateDatabaseVariant.class, mode = Mode.INCLUDE,
            names = {"POSTGRES_WITH_NEW_DNS_ZONE", "FLEXIBLE_POSTGRES_WITH_DELEGATED_SUBNET_AND_NEW_DNS_ZONE"})
    public void testCheckOrCreateWhenNetworkLinkNotExistsAndNotRequestedAndNotCreatedInDatabaseAndError(PrivateDatabaseVariant variant) {
        when(azureResourceIdProviderService.generateDeploymentId(any(), any(), any())).thenReturn(DEPLOYMENT_ID);
        when(azureResourceDeploymentHelperService.getAzureNetwork(any(), any(), any())).thenReturn(mock(Network.class));
        when(client.getCurrentSubscription()).thenReturn(mock(Subscription.class));
        when(client.getCurrentSubscription().subscriptionId()).thenReturn(SUBSCRIPTION_ID);
        when(client.checkIfNetworkLinksDeployed(any(), any(), any())).thenReturn(false);
        when(azureResourcePersistenceHelperService.isRequested(DEPLOYMENT_ID, AZURE_VIRTUAL_NETWORK_LINK)).thenReturn(false);
        when(azureResourcePersistenceHelperService.isCreated(DEPLOYMENT_ID, AZURE_VIRTUAL_NETWORK_LINK)).thenReturn(false);
        doThrow(new CloudConnectorException("text", null)).when(azureResourceDeploymentHelperService).deployTemplate(any(), any());

        CloudConnectorException exception = assertThrows(CloudConnectorException.class, () -> {
            underTest.checkOrCreateNetworkLinks(ac, client, getNetworkView(), RESOURCE_GROUP, Collections.emptyMap(), Set.of(), variant);
        });

        assertEquals("text", exception.getMessage());

        verify(azureResourcePersistenceHelperService, times(1)).persistCloudResource(any(), any(), any(), any());
        verify(azureResourcePersistenceHelperService, times(0)).updateCloudResource(any(), any(), any(), any(), any());
        verify(azureResourcePersistenceHelperService, times(1)).isRequested(any(), any());
        verify(azureResourcePersistenceHelperService, times(1)).isCreated(any(), any());
        verify(azureResourceDeploymentHelperService, times(1)).pollForCreation(any(), any());
    }

    private AzureNetworkView getNetworkView() {
        com.sequenceiq.cloudbreak.cloud.model.Network network = new com.sequenceiq.cloudbreak.cloud.model.Network(null);
        network.putParameter("subnets", "subnet");
        AzureNetworkView networkView = new AzureNetworkView(network);
        networkView.setExistingNetwork(false);
        networkView.setNetworkId(NETWORK_ID);
        networkView.setResourceGroupName(NETWORK_RG);

        return networkView;
    }
}