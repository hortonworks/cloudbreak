package com.sequenceiq.cloudbreak.job.provider;

import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_INSTANCE;
import static com.sequenceiq.common.model.ProviderSyncState.BASIC_SKU_MIGRATION_NEEDED;
import static com.sequenceiq.common.model.ProviderSyncState.OUTBOUND_UPGRADE_NEEDED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.NetworkAttributes;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.SkuAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.converter.spi.CloudContextProvider;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.common.api.type.OutboundType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class ProviderSyncServiceTest {

    private static final String INSTANCE_1 = "instance1";

    private static final String INSTANCE_2 = "instance2";

    private static final String IP_1 = "ip1";

    private static final String IP_2 = "ip2";

    private static final String NETWORK_1 = "network1";

    private static final String NATGW_VNET_001 = "natgw-vnet-001";

    private static final String INSTANCE_ID = "instanceId";

    private static final String TEST_GROUP = "test";

    private static final String TEST_EXCEPTION_MESSAGE = "Test Exception";

    private static final String NO_SKU_ATTRIBUTES_MESSAGE = "No SKU attributes";

    private static final String EXTERNAL_RESOURCE_ATTRIBUTES_MESSAGE = "Resource has ExternalResourceAttributes, not NetworkAttributes";

    @Mock
    private ProviderSyncConfig providerSyncConfig;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ResourceNotifier resourceNotifier;

    @Mock
    private CloudContextProvider cloudContextProvider;

    @Mock
    private CredentialClientService credentialClientService;

    @InjectMocks
    private ProviderSyncService underTest;

    @Mock
    private StackDto stack;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private ResourceConnector resourceConnector;

    @BeforeEach
    void setup() {
        lenient().when(cloudContextProvider.getCloudContext(stack)).thenReturn(cloudContext);
        lenient().when(credentialClientService.getCloudCredential(stack.getEnvironmentCrn())).thenReturn(cloudCredential);
        lenient().when(cloudPlatformConnectors.get(cloudContext.getPlatformVariant())).thenReturn(cloudConnector);
        Authenticator authenticator = mock(Authenticator.class);
        lenient().when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        lenient().when(cloudConnector.authentication()).thenReturn(authenticator);
        lenient().when(cloudConnector.resources()).thenReturn(resourceConnector);
        lenient().when(providerSyncConfig.getResourceTypeList()).thenReturn(Set.of(AZURE_INSTANCE, ResourceType.AZURE_NETWORK));

        // Set up lenient stubbing for resourceConnector to avoid strict argument matching issues
        lenient().when(resourceConnector.checkForSyncer(any(AuthenticatedContext.class), any(List.class)))
                .thenReturn(List.of());
    }

    @Test
    @DisplayName("Test that syncResources successfully syncs filtered cloud resources and notifies updates")
    void testSyncResources() {
        List<CloudResource> cloudResources = List.of(
                createCloudResource(INSTANCE_1, AZURE_INSTANCE),
                createCloudResource(INSTANCE_2, AZURE_INSTANCE),
                createCloudResource(IP_1, ResourceType.AZURE_PUBLIC_IP),
                createCloudResource(IP_2, ResourceType.AZURE_PUBLIC_IP));

        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(cloudResources);
        List<CloudResource> filteredList = cloudResources.stream().filter(r -> r.getType() == AZURE_INSTANCE).toList();
        List<CloudResourceStatus> filteredResourceStatusList = filteredList.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .toList();
        when(resourceConnector.checkForSyncer(authenticatedContext, filteredList)).thenReturn(filteredResourceStatusList);

        underTest.syncResources(stack);

        verify(resourceNotifier, times(1)).notifyUpdates(filteredList, cloudContext);
    }

    @Test
    @DisplayName("Test that syncResources properly handles exceptions and doesn't notify updates when resource service fails")
    void testSyncResourcesHandlesException() {
        when(resourceService.getAllCloudResource(anyLong())).thenThrow(new CloudbreakServiceException(TEST_EXCEPTION_MESSAGE));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.syncResources(stack));
        assertEquals(TEST_EXCEPTION_MESSAGE, exception.getMessage());
        verify(resourceNotifier, never()).notifyUpdates(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Basic", "Standard"})
    @DisplayName("Test provider sync status setting based on SKU type - Basic SKU should trigger migration, Standard should clean up states")
    void setProviderSyncStatusWithSku(String sku) throws CloudbreakServiceException {
        CloudResource cloudResource = mock(CloudResource.class);
        SkuAttributes skuAttributes = new SkuAttributes();
        skuAttributes.setSku(sku);
        when(cloudResource.getParameter(CloudResource.ATTRIBUTES, SkuAttributes.class)).thenReturn(skuAttributes);
        List<CloudResource> resourceList = List.of(cloudResource);
        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(resourceList);
        when(cloudResource.getType()).thenReturn(AZURE_INSTANCE);
        List<CloudResourceStatus> resourceStatusList = resourceList.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .toList();
        when(resourceConnector.checkForSyncer(authenticatedContext, resourceList)).thenReturn(resourceStatusList);

        underTest.syncResources(stack);

        if ("Basic".equalsIgnoreCase(sku)) {
            verify(stackUpdater, times(1)).addProviderState(stack.getId(), BASIC_SKU_MIGRATION_NEEDED);
        } else {
            verify(stackUpdater, times(1)).removeProviderStates(stack.getId(), Set.of(OUTBOUND_UPGRADE_NEEDED,
                    BASIC_SKU_MIGRATION_NEEDED));
        }
    }

    @Test
    @DisplayName("Test that provider sync status properly handles exceptions when getting SKU attributes and removes provider states")
    void setProviderSyncStatusWithException() throws CloudbreakServiceException {
        CloudResource cloudResource = mock(CloudResource.class);
        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(List.of(cloudResource));
        when(cloudResource.getParameter(CloudResource.ATTRIBUTES, SkuAttributes.class)).thenThrow(new CloudbreakServiceException(TEST_EXCEPTION_MESSAGE));
        List<CloudResource> resourceList = List.of(cloudResource);
        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(resourceList);
        when(cloudResource.getType()).thenReturn(AZURE_INSTANCE);

        List<CloudResourceStatus> resourceStatusList = resourceList.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .toList();
        when(resourceConnector.checkForSyncer(authenticatedContext, resourceList)).thenReturn(resourceStatusList);

        underTest.syncResources(stack);

        verify(stackUpdater, times(1)).removeProviderStates(stack.getId(), Set.of(OUTBOUND_UPGRADE_NEEDED,
                BASIC_SKU_MIGRATION_NEEDED));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NOT_DEFINED", "DEFAULT"})
    @DisplayName("Test that upgradeable outbound types (NOT_DEFINED, DEFAULT) trigger outbound upgrade state")
    void testSetProviderSyncStatusWithUpgradeableOutboundType(String outboundTypeStr) throws CloudbreakServiceException {
        // Given
        List<CloudResource> resourceList = getCloudResourceList(outboundTypeStr);

        List<CloudResourceStatus> resourceStatusList = resourceList.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .toList();
        when(resourceConnector.checkForSyncer(authenticatedContext, resourceList)).thenReturn(resourceStatusList);

        // When
        underTest.syncResources(stack);

        // Then
        verify(stackUpdater, times(1)).addProviderState(stack.getId(), OUTBOUND_UPGRADE_NEEDED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"LOAD_BALANCER", "PUBLIC_IP", "USER_ASSIGNED_NATGATEWAY", "USER_DEFINED_ROUTING"})
    @DisplayName("Test that non-upgradeable outbound types remove provider sync states instead of triggering upgrades")
    void testSetProviderSyncStatusWithNonUpgradeableOutboundType(String outboundTypeStr) throws CloudbreakServiceException {
        // Given
        List<CloudResource> resourceList = getCloudResourceList(outboundTypeStr);

        List<CloudResourceStatus> resourceStatusList = resourceList.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .toList();
        when(resourceConnector.checkForSyncer(authenticatedContext, resourceList)).thenReturn(resourceStatusList);

        // When
        underTest.syncResources(stack);

        // Then
        verify(stackUpdater, times(1)).removeProviderStates(stack.getId(),
                Set.of(OUTBOUND_UPGRADE_NEEDED, BASIC_SKU_MIGRATION_NEEDED));
    }

    @Test
    @DisplayName("Test Azure network present in both lists with upgradeable outbound type triggers upgrade without duplicate processing")
    void testAzureNetworkInBothListsUpgradeable() throws CloudbreakServiceException {
        // Given: AZURE_NETWORK with upgradeable outbound type in both original and synced resources
        CloudResource azureNetworkOriginal = createAzureNetworkResource("DEFAULT");
        CloudResource azureNetworkSynced = createAzureNetworkResource("DEFAULT");
        CloudResource otherResource = createCloudResource(INSTANCE_1, AZURE_INSTANCE);

        List<CloudResource> originalResources = List.of(azureNetworkOriginal, otherResource);
        List<CloudResource> syncedResources = List.of(azureNetworkSynced);

        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(originalResources);
        List<CloudResourceStatus> resourceStatusList = syncedResources.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .toList();
        when(resourceConnector.checkForSyncer(authenticatedContext, List.of(azureNetworkOriginal, otherResource))).thenReturn(resourceStatusList);

        // When
        underTest.syncResources(stack);

        // Then: Should detect upgrade needed from synced resources only (no duplicate check)
        verify(stackUpdater, times(1)).addProviderState(stack.getId(), OUTBOUND_UPGRADE_NEEDED);
    }

    @Test
    @DisplayName("Test Azure network present in both lists with non-upgradeable outbound type removes provider states")
    void testAzureNetworkInBothListsNonUpgradeable() throws CloudbreakServiceException {
        // Given: AZURE_NETWORK with non-upgradeable outbound type in both lists
        CloudResource azureNetworkOriginal = createAzureNetworkResource("USER_ASSIGNED_NATGATEWAY");
        CloudResource azureNetworkSynced = createAzureNetworkResource("USER_ASSIGNED_NATGATEWAY");
        CloudResource otherResource = createCloudResource(INSTANCE_1, AZURE_INSTANCE);

        List<CloudResource> originalResources = List.of(azureNetworkOriginal, otherResource);
        List<CloudResource> syncedResources = List.of(azureNetworkSynced);

        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(originalResources);
        List<CloudResourceStatus> resourceStatusList = syncedResources.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .toList();
        when(resourceConnector.checkForSyncer(authenticatedContext, List.of(azureNetworkOriginal, otherResource))).thenReturn(resourceStatusList);

        // When
        underTest.syncResources(stack);

        // Then: Should remove provider states since no upgrade needed
        verify(stackUpdater, times(1)).removeProviderStates(stack.getId(),
                Set.of(OUTBOUND_UPGRADE_NEEDED, BASIC_SKU_MIGRATION_NEEDED));
    }

    @Test
    @DisplayName("Test Azure network only in original list with upgradeable outbound type triggers upgrade from unsynced resources")
    void testAzureNetworkOnlyInOriginalListUpgradeable() throws CloudbreakServiceException {
        // Given: AZURE_NETWORK only in original resources with upgradeable outbound type
        CloudResource azureNetworkOriginal = createAzureNetworkResource("DEFAULT");
        CloudResource otherResourceOriginal = createCloudResource(INSTANCE_1, AZURE_INSTANCE);
        CloudResource otherResourceSynced = createCloudResource(INSTANCE_2, AZURE_INSTANCE);

        List<CloudResource> originalResources = List.of(azureNetworkOriginal, otherResourceOriginal);
        List<CloudResource> syncedResources = List.of(otherResourceSynced);

        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(originalResources);
        List<CloudResourceStatus> resourceStatusList = syncedResources.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .toList();
        when(resourceConnector.checkForSyncer(authenticatedContext, List.of(azureNetworkOriginal, otherResourceOriginal))).thenReturn(resourceStatusList);

        // When
        underTest.syncResources(stack);

        // Then: Should detect upgrade needed from unsync resources check
        verify(stackUpdater, times(1)).addProviderState(stack.getId(), OUTBOUND_UPGRADE_NEEDED);
    }

    @Test
    @DisplayName("Test Azure network only in original list with non-upgradeable outbound type removes provider states")
    void testAzureNetworkOnlyInOriginalListNonUpgradeable() throws CloudbreakServiceException {
        // Given: AZURE_NETWORK only in original resources with non-upgradeable outbound type
        CloudResource azureNetworkOriginal = createAzureNetworkResource("LOAD_BALANCER");
        CloudResource otherResourceOriginal = createCloudResource(INSTANCE_1, AZURE_INSTANCE);
        CloudResource otherResourceSynced = createCloudResource(INSTANCE_2, AZURE_INSTANCE);

        List<CloudResource> originalResources = List.of(azureNetworkOriginal, otherResourceOriginal);
        List<CloudResource> syncedResources = List.of(otherResourceSynced);

        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(originalResources);
        List<CloudResourceStatus> resourceStatusList = syncedResources.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .toList();
        when(resourceConnector.checkForSyncer(authenticatedContext, List.of(azureNetworkOriginal, otherResourceOriginal))).thenReturn(resourceStatusList);

        // When
        underTest.syncResources(stack);

        // Then: Should remove provider states since no upgrade needed
        verify(stackUpdater, times(1)).removeProviderStates(stack.getId(),
                Set.of(OUTBOUND_UPGRADE_NEEDED, BASIC_SKU_MIGRATION_NEEDED));
    }

    @Test
    @DisplayName("Test Azure network only in synced list with upgradeable outbound type triggers upgrade from synced resources")
    void testAzureNetworkOnlyInSyncedListUpgradeable() throws CloudbreakServiceException {
        // Given: AZURE_NETWORK only in synced resources with upgradeable outbound type
        CloudResource azureNetworkSynced = createAzureNetworkResource("NOT_DEFINED");
        CloudResource otherResourceOriginal = createCloudResource(INSTANCE_1, AZURE_INSTANCE);
        CloudResource otherResourceSynced = createCloudResource(INSTANCE_2, AZURE_INSTANCE);

        List<CloudResource> originalResources = List.of(otherResourceOriginal);
        List<CloudResource> syncedResources = List.of(azureNetworkSynced, otherResourceSynced);

        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(originalResources);
        List<CloudResourceStatus> resourceStatusList = syncedResources.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .toList();
        when(resourceConnector.checkForSyncer(authenticatedContext, originalResources)).thenReturn(resourceStatusList);

        // When
        underTest.syncResources(stack);

        // Then: Should detect upgrade needed from synced resources check
        verify(stackUpdater, times(1)).addProviderState(stack.getId(), OUTBOUND_UPGRADE_NEEDED);
    }

    @Test
    @DisplayName("Test Azure network only in synced list with non-upgradeable outbound type removes provider states")
    void testAzureNetworkOnlyInSyncedListNonUpgradeable() throws CloudbreakServiceException {
        // Given: AZURE_NETWORK only in synced resources with non-upgradeable outbound type
        CloudResource azureNetworkSynced = createAzureNetworkResource("PUBLIC_IP");
        CloudResource otherResourceOriginal = createCloudResource(INSTANCE_1, AZURE_INSTANCE);
        CloudResource otherResourceSynced = createCloudResource(INSTANCE_2, AZURE_INSTANCE);

        List<CloudResource> originalResources = List.of(otherResourceOriginal);
        List<CloudResource> syncedResources = List.of(azureNetworkSynced, otherResourceSynced);

        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(originalResources);
        List<CloudResourceStatus> resourceStatusList = syncedResources.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .toList();
        when(resourceConnector.checkForSyncer(authenticatedContext, originalResources)).thenReturn(resourceStatusList);

        // When
        underTest.syncResources(stack);

        // Then: Should remove provider states since no upgrade needed
        verify(stackUpdater, times(1)).removeProviderStates(stack.getId(),
                Set.of(OUTBOUND_UPGRADE_NEEDED, BASIC_SKU_MIGRATION_NEEDED));
    }

    @Test
    @DisplayName("Test that when no Azure network is present in either list, provider states are removed")
    void testNoAzureNetworkInEitherList() throws CloudbreakServiceException {
        // Given: No AZURE_NETWORK in either original or synced resources
        CloudResource instanceOriginal = createCloudResource(INSTANCE_1, AZURE_INSTANCE);
        CloudResource instanceSynced = createCloudResource(INSTANCE_2, AZURE_INSTANCE);

        List<CloudResource> originalResources = List.of(instanceOriginal);
        List<CloudResource> syncedResources = List.of(instanceSynced);

        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(originalResources);
        List<CloudResourceStatus> resourceStatusList = syncedResources.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .toList();
        when(resourceConnector.checkForSyncer(authenticatedContext, originalResources)).thenReturn(resourceStatusList);

        // When
        underTest.syncResources(stack);

        // Then: Should remove provider states since no network attributes to check
        verify(stackUpdater, times(1)).removeProviderStates(stack.getId(),
                Set.of(OUTBOUND_UPGRADE_NEEDED, BASIC_SKU_MIGRATION_NEEDED));
    }

    @Test
    @DisplayName("Test same network with upgradeable type in synced resources and non-upgradeable in original triggers upgrade")
    void testSameNetworkWithUpgradeableInSyncedAndNonUpgradeableInOriginal() throws CloudbreakServiceException {
        // Given: AZURE_NETWORK with different outbound types - upgradeable in synced, non-upgradeable in original
        CloudResource azureNetworkOriginal = createAzureNetworkResource("LOAD_BALANCER");
        CloudResource azureNetworkSynced = createAzureNetworkResource("DEFAULT");
        CloudResource otherResource = createCloudResource(INSTANCE_1, AZURE_INSTANCE);

        List<CloudResource> originalResources = List.of(azureNetworkOriginal, otherResource);
        List<CloudResource> syncedResources = List.of(azureNetworkSynced);

        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(originalResources);
        List<CloudResourceStatus> resourceStatusList = syncedResources.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .toList();
        when(resourceConnector.checkForSyncer(authenticatedContext, List.of(azureNetworkOriginal, otherResource))).thenReturn(resourceStatusList);

        // When
        underTest.syncResources(stack);

        // Then: Should detect upgrade needed from synced resources (checked first)
        // Original network with non-upgradeable type should be filtered out from second check
        verify(stackUpdater, times(1)).addProviderState(stack.getId(), OUTBOUND_UPGRADE_NEEDED);
    }

    @Test
    @DisplayName("Test same network with non-upgradeable type in synced resources and upgradeable in original removes states due to filtering")
    void testSameNetworkWithNonUpgradeableInSyncedAndUpgradeableInOriginal() throws CloudbreakServiceException {
        // Given: AZURE_NETWORK with different outbound types - non-upgradeable in synced, upgradeable in original
        CloudResource azureNetworkOriginal = createAzureNetworkResource("DEFAULT");
        CloudResource azureNetworkSynced = createAzureNetworkResource("USER_ASSIGNED_NATGATEWAY");
        CloudResource otherResource = createCloudResource(INSTANCE_1, AZURE_INSTANCE);

        List<CloudResource> originalResources = List.of(azureNetworkOriginal, otherResource);
        List<CloudResource> syncedResources = List.of(azureNetworkSynced);

        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(originalResources);
        List<CloudResourceStatus> resourceStatusList = syncedResources.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .toList();
        when(resourceConnector.checkForSyncer(authenticatedContext, List.of(azureNetworkOriginal, otherResource))).thenReturn(resourceStatusList);

        // When
        underTest.syncResources(stack);

        // Then: Should remove provider states since synced network (checked first) is not upgradeable
        // Original network with upgradeable type should be filtered out from second check
        verify(stackUpdater, times(1)).removeProviderStates(stack.getId(),
                Set.of(OUTBOUND_UPGRADE_NEEDED, BASIC_SKU_MIGRATION_NEEDED));
    }

    @Test
    @DisplayName("Test that resources without NetworkAttributes (NAT Gateway, instances) return Optional.empty and remove provider states")
    void testResourcesWithoutNetworkAttributesReturnOptionalEmpty() throws CloudbreakServiceException {
        // Given: Resources that don't have NetworkAttributes (NAT Gateway with ExternalResourceAttributes, Instance without any attributes)
        CloudResource natGateway = createCloudResourceWithExternalAttributes(NATGW_VNET_001);
        CloudResource instance = createCloudResource(INSTANCE_1, AZURE_INSTANCE);

        List<CloudResource> originalResources = List.of(natGateway, instance);
        List<CloudResource> syncedResources = List.of(natGateway);

        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(originalResources);
        List<CloudResourceStatus> resourceStatusList = syncedResources.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .toList();

        // When
        underTest.syncResources(stack);

        // Then: Should remove provider states since no resources have upgradeable NetworkAttributes
        verify(stackUpdater, times(1)).removeProviderStates(stack.getId(),
                Set.of(OUTBOUND_UPGRADE_NEEDED, BASIC_SKU_MIGRATION_NEEDED));
    }

    @Test
    @DisplayName("Test mixed resources with some having NetworkAttributes and others without, where NetworkAttributes has non-upgradeable type")
    void testMixedResourcesWithAndWithoutNetworkAttributes() throws CloudbreakServiceException {
        // Given: Mix of resources - some with NetworkAttributes, some without
        CloudResource natGateway = createCloudResourceWithExternalAttributes(NATGW_VNET_001);
        CloudResource azureNetwork = createAzureNetworkResource("USER_ASSIGNED_NATGATEWAY");
        CloudResource instance = createCloudResource(INSTANCE_1, AZURE_INSTANCE);

        List<CloudResource> originalResources = List.of(natGateway, azureNetwork, instance);
        List<CloudResource> syncedResources = List.of(natGateway, azureNetwork);

        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(originalResources);
        List<CloudResourceStatus> resourceStatusList = syncedResources.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .toList();

        // When
        underTest.syncResources(stack);

        // Then: Should remove provider states since the only NetworkAttributes has non-upgradeable outbound type
        verify(stackUpdater, times(1)).removeProviderStates(stack.getId(),
                Set.of(OUTBOUND_UPGRADE_NEEDED, BASIC_SKU_MIGRATION_NEEDED));
    }

    @Test
    @DisplayName("Test mixed resources where NetworkAttributes has upgradeable type triggers upgrade despite other resources lacking attributes")
    void testMixedResourcesWithUpgradeableNetworkAttributes() throws CloudbreakServiceException {
        // Given: Mix of resources - some with NetworkAttributes (upgradeable), some without
        CloudResource natGateway = createCloudResourceWithExternalAttributes(NATGW_VNET_001);
        CloudResource azureNetwork = createAzureNetworkResource("DEFAULT");
        CloudResource instance = createCloudResource(INSTANCE_1, AZURE_INSTANCE);

        List<CloudResource> originalResources = List.of(natGateway, azureNetwork, instance);
        List<CloudResource> syncedResources = List.of(natGateway, azureNetwork);

        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(originalResources);
        List<CloudResourceStatus> resourceStatusList = syncedResources.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .toList();

        // When
        underTest.syncResources(stack);

        // Then: Should detect upgrade needed from the upgradeable NetworkAttributes
        verify(stackUpdater, times(1)).addProviderState(stack.getId(), OUTBOUND_UPGRADE_NEEDED);
    }

    @Test
    @DisplayName("Test real-world scenario with NAT Gateway and Network having USER_ASSIGNED_NATGATEWAY removes provider states")
    void testRealWorldScenarioWithNatGatewayAndNetwork() throws CloudbreakServiceException {
        // Given: Real-world scenario similar to the log - NAT Gateway + Network with USER_ASSIGNED_NATGATEWAY
        CloudResource natGateway = createCloudResourceWithExternalAttributes("natgw-001");
        CloudResource azureNetwork = createAzureNetworkResourceWithDetails("USER_ASSIGNED_NATGATEWAY");

        List<CloudResource> originalResources = List.of(natGateway, azureNetwork);
        List<CloudResource> syncedResources = List.of(natGateway, azureNetwork);

        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(originalResources);
        List<CloudResourceStatus> resourceStatusList = syncedResources.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .toList();

        // When
        underTest.syncResources(stack);

        // Then: Should remove provider states since USER_ASSIGNED_NATGATEWAY is not upgradeable
        verify(stackUpdater, times(1)).removeProviderStates(stack.getId(),
                Set.of(OUTBOUND_UPGRADE_NEEDED, BASIC_SKU_MIGRATION_NEEDED));
    }

    @Test
    @DisplayName("Test resource with NetworkAttributes but null outbound type defaults to NOT_DEFINED and triggers upgrade")
    void testResourceWithNetworkAttributesButNullOutboundType() throws CloudbreakServiceException {
        // Given: Resource with NetworkAttributes but outboundType is null (should default to NOT_DEFINED)
        CloudResource azureNetworkWithNullOutbound = createAzureNetworkResourceWithNullOutboundType();

        List<CloudResource> originalResources = List.of(azureNetworkWithNullOutbound);
        List<CloudResource> syncedResources = List.of(azureNetworkWithNullOutbound);

        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(originalResources);
        List<CloudResourceStatus> resourceStatusList = syncedResources.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .toList();
        when(resourceConnector.checkForSyncer(authenticatedContext, originalResources)).thenReturn(resourceStatusList);

        // When
        underTest.syncResources(stack);

        // Then: Should detect upgrade needed since null outboundType defaults to NOT_DEFINED (upgradeable)
        verify(stackUpdater, times(1)).addProviderState(stack.getId(), OUTBOUND_UPGRADE_NEEDED);
    }

    private CloudResource createAzureNetworkResource(String outboundTypeStr) {
        CloudResource cloudResource = mock(CloudResource.class);
        NetworkAttributes networkAttributes = new NetworkAttributes();
        OutboundType outboundType = OutboundType.valueOf(outboundTypeStr);
        networkAttributes.setOutboundType(outboundType);

        lenient().when(cloudResource.getName()).thenReturn(NETWORK_1);
        lenient().when(cloudResource.getParameter(CloudResource.ATTRIBUTES, SkuAttributes.class)).thenThrow(
                new CloudbreakServiceException(NO_SKU_ATTRIBUTES_MESSAGE));
        lenient().when(cloudResource.getType()).thenReturn(ResourceType.AZURE_NETWORK);
        lenient().when(cloudResource.getParameter(CloudResource.ATTRIBUTES, NetworkAttributes.class)).thenReturn(networkAttributes);
        when(cloudResource.getDetailedInfo()).thenReturn("AZURE_NETWORK - " + NETWORK_1);

        return cloudResource;
    }

    private CloudResource createCloudResource(String name, ResourceType resourceType) {
        return CloudResource.builder()
                .withName(name)
                .withStatus(CREATED)
                .withType(resourceType)
                .withInstanceId(INSTANCE_ID)
                .withGroup(TEST_GROUP)
                .build();
    }

    private List<CloudResource> getCloudResourceList(String outboundTypeStr) throws CloudbreakServiceException {
        CloudResource cloudResource = mock(CloudResource.class);
        NetworkAttributes networkAttributes = new NetworkAttributes();
        OutboundType outboundType = OutboundType.valueOf(outboundTypeStr);
        networkAttributes.setOutboundType(outboundType);

        when(cloudResource.getParameter(CloudResource.ATTRIBUTES, NetworkAttributes.class)).thenReturn(networkAttributes);
        when(cloudResource.getParameter(CloudResource.ATTRIBUTES, SkuAttributes.class)).thenThrow(new CloudbreakServiceException(NO_SKU_ATTRIBUTES_MESSAGE));
        when(cloudResource.getType()).thenReturn(AZURE_INSTANCE);
        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(List.of(cloudResource));

        return List.of(cloudResource);
    }

    private CloudResource createCloudResourceWithExternalAttributes(String name) {
        CloudResource cloudResource = mock(CloudResource.class);

        lenient().when(cloudResource.getName()).thenReturn(name);
        lenient().when(cloudResource.getType()).thenReturn(ResourceType.AZURE_NAT_GATEWAY);
        lenient().when(cloudResource.getReference()).thenReturn("/subscriptions/test/resourceGroups/test/providers/Microsoft.Network/natGateways/" + name);

        // Simulate that this resource has ExternalResourceAttributes, not NetworkAttributes
        lenient().when(cloudResource.getParameter(CloudResource.ATTRIBUTES, NetworkAttributes.class))
                .thenThrow(new CloudbreakServiceException(EXTERNAL_RESOURCE_ATTRIBUTES_MESSAGE));
        lenient().when(cloudResource.getParameter(CloudResource.ATTRIBUTES, SkuAttributes.class))
                .thenThrow(new CloudbreakServiceException(NO_SKU_ATTRIBUTES_MESSAGE));

        lenient().when(cloudResource.getDetailedInfo()).thenReturn("AZURE_NAT_GATEWAY - " + name);

        return cloudResource;
    }

    private CloudResource createAzureNetworkResourceWithDetails(String outboundTypeStr) {
        CloudResource cloudResource = mock(CloudResource.class);
        NetworkAttributes networkAttributes = new NetworkAttributes();
        networkAttributes.setSubnetId("subnet-live");
        networkAttributes.setCloudPlatform("AZURE");
        networkAttributes.setResourceGroupName("rg-network");
        networkAttributes.setNetworkId("vnet-001");
        OutboundType outboundType = OutboundType.valueOf(outboundTypeStr);
        networkAttributes.setOutboundType(outboundType);

        lenient().when(cloudResource.getName()).thenReturn("vnet-001");
        lenient().when(cloudResource.getType()).thenReturn(ResourceType.AZURE_NETWORK);
        lenient().when(cloudResource.getReference()).thenReturn(null);
        lenient().when(cloudResource.getParameter(CloudResource.ATTRIBUTES, NetworkAttributes.class)).thenReturn(networkAttributes);
        lenient().when(cloudResource.getParameter(CloudResource.ATTRIBUTES, SkuAttributes.class))
                .thenThrow(new CloudbreakServiceException(NO_SKU_ATTRIBUTES_MESSAGE));
        lenient().when(cloudResource.getDetailedInfo()).thenReturn("AZURE_NETWORK - " + "vnet-001");

        return cloudResource;
    }

    private CloudResource createAzureNetworkResourceWithNullOutboundType() {
        CloudResource cloudResource = mock(CloudResource.class);
        NetworkAttributes networkAttributes = new NetworkAttributes();
        // Don't set outboundType - it will be null and getOutboundType() should return NOT_DEFINED

        lenient().when(cloudResource.getName()).thenReturn(NETWORK_1);
        lenient().when(cloudResource.getType()).thenReturn(ResourceType.AZURE_NETWORK);
        lenient().when(cloudResource.getParameter(CloudResource.ATTRIBUTES, NetworkAttributes.class)).thenReturn(networkAttributes);
        lenient().when(cloudResource.getParameter(CloudResource.ATTRIBUTES, SkuAttributes.class))
                .thenThrow(new CloudbreakServiceException(NO_SKU_ATTRIBUTES_MESSAGE));
        lenient().when(cloudResource.getDetailedInfo()).thenReturn("AZURE_NETWORK - " + NETWORK_1);

        return cloudResource;
    }
}
