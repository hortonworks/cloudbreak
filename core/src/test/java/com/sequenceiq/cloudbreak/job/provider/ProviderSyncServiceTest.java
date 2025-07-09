package com.sequenceiq.cloudbreak.job.provider;

import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_INSTANCE;
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
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.SkuAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.converter.spi.CloudContextProvider;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.ProviderSyncState;

@ExtendWith(MockitoExtension.class)
class ProviderSyncServiceTest {

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
        lenient().when(providerSyncConfig.getResourceTypeList()).thenReturn(Set.of(AZURE_INSTANCE));

    }

    @Test
    void testSyncResources() {
        List<CloudResource> cloudResources = List.of(
                createCloudResource("instance1", AZURE_INSTANCE),
                createCloudResource("instance2", AZURE_INSTANCE),
                createCloudResource("ip1", ResourceType.AZURE_PUBLIC_IP),
                createCloudResource("ip2", ResourceType.AZURE_PUBLIC_IP));

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
    void testSyncResourcesHandlesException() {
        when(resourceService.getAllCloudResource(anyLong())).thenThrow(new CloudbreakServiceException("Test Exception"));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.syncResources(stack));
        assertEquals("Test Exception", exception.getMessage());
        verify(resourceNotifier, never()).notifyUpdates(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Basic", "Standard"})
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

        ProviderSyncState syncState = "Basic".equalsIgnoreCase(sku) ?
                ProviderSyncState.BASIC_SKU_MIGRATION_NEEDED :
                ProviderSyncState.VALID;
        verify(stackUpdater, times(1)).updateProviderState(stack.getId(), Set.of(syncState));
    }

    @Test
    void setProviderSyncStatusWithException() throws CloudbreakServiceException {
        CloudResource cloudResource = mock(CloudResource.class);
        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(List.of(cloudResource));
        when(cloudResource.getParameter(CloudResource.ATTRIBUTES, SkuAttributes.class)).thenThrow(new CloudbreakServiceException("Test Exception"));
        List<CloudResource> resourceList = List.of(cloudResource);
        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(resourceList);
        when(cloudResource.getType()).thenReturn(AZURE_INSTANCE);

        List<CloudResourceStatus> resourceStatusList = resourceList.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .toList();
        when(resourceConnector.checkForSyncer(authenticatedContext, resourceList)).thenReturn(resourceStatusList);

        underTest.syncResources(stack);

        verify(stackUpdater, times(1)).updateProviderState(stack.getId(), Set.of(ProviderSyncState.VALID));
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