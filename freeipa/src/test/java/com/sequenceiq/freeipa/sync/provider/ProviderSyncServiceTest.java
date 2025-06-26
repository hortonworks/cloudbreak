package com.sequenceiq.freeipa.sync.provider;

import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@ExtendWith(MockitoExtension.class)
class ProviderSyncServiceTest {

    private static final String ENVIRONMENT_CRN = "envCrn";

    @Mock
    private ProviderSyncConfig providerSyncConfig;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ResourceNotifier resourceNotifier;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private CredentialService credentialService;

    @Mock
    private Stack stack;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @InjectMocks
    private ProviderSyncService underTest;

    @Test
    void testSyncResources() {
        when(credentialService.getCredentialByEnvCrn(eq(ENVIRONMENT_CRN))).thenReturn(new Credential(null, null, null, null, null));
        when(credentialConverter.convert(any())).thenReturn(cloudCredential);
        CloudPlatformVariant platformVariant = new CloudPlatformVariant(CloudPlatform.AZURE.name(), CloudPlatform.AZURE.name());
//        when(cloudContext.getPlatformVariant()).thenReturn(platformVariant);
        when(cloudPlatformConnectors.get(platformVariant)).thenReturn(cloudConnector);
        Authenticator authenticator = mock(Authenticator.class);
        when(authenticator.authenticate(any(), eq(cloudCredential))).thenReturn(authenticatedContext);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(stack.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        when(stack.getPlatformvariant()).thenReturn("AZURE");
        when(stack.getCloudPlatform()).thenReturn("AZURE");
        List<CloudResource> cloudResources = List.of(
                createCloudResource("instance1", AZURE_INSTANCE),
                createCloudResource("instance2", AZURE_INSTANCE),
                createCloudResource("ip1", ResourceType.AZURE_PUBLIC_IP),
                createCloudResource("ip2", ResourceType.AZURE_PUBLIC_IP));

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

        verify(resourceNotifier, times(1)).notifyUpdates(eq(filteredList), any(CloudContext.class));
    }

    @Test
    void testSyncResourcesHandlesException() {
        when(resourceService.getAllCloudResource(anyLong())).thenThrow(new CloudbreakServiceException("Test Exception"));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.syncResources(stack));
        assertEquals("Test Exception", exception.getMessage());
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