package com.sequenceiq.cloudbreak.job.provider;

import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.converter.spi.CloudContextProvider;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.common.api.type.ResourceType;

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

    @Test
    void testSyncResources() {
        when(cloudContextProvider.getCloudContext(stack)).thenReturn(cloudContext);
        when(credentialClientService.getCloudCredential(stack.getEnvironmentCrn())).thenReturn(cloudCredential);
        when(cloudPlatformConnectors.get(cloudContext.getPlatformVariant())).thenReturn(cloudConnector);
        Authenticator authenticator = mock(Authenticator.class);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        List<CloudResource> cloudResources = List.of(
                createCloudResource("instance1", AZURE_INSTANCE),
                createCloudResource("instance2", AZURE_INSTANCE),
                createCloudResource("ip1", ResourceType.AZURE_PUBLIC_IP),
                createCloudResource("ip2", ResourceType.AZURE_PUBLIC_IP));

        List<CloudResourceStatus> resourceStatusList = cloudResources.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .toList();
        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(cloudResources);
        when(providerSyncConfig.getResourceTypeList()).thenReturn(Set.of(AZURE_INSTANCE));
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        List<CloudResource> filteredList = cloudResources.stream().filter(r -> r.getType() == AZURE_INSTANCE).toList();
        List<CloudResourceStatus> filteredResourceStatusList = filteredList.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .toList();
        when(resourceConnector.check(authenticatedContext, filteredList)).thenReturn(filteredResourceStatusList);

        underTest.syncResources(stack);

        verify(resourceNotifier, times(1)).notifyUpdates(filteredList, cloudContext);
    }

    @Test
    void testSyncResourcesHandlesException() {
        when(resourceService.getAllCloudResource(anyLong())).thenThrow(new CloudbreakServiceException("Test Exception"));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.syncResources(stack));
        assertEquals("Test Exception", exception.getMessage());
        verify(resourceNotifier, never()).notifyUpdates(any(), any());

        verify(resourceNotifier, never()).notifyUpdates(any(), any());
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