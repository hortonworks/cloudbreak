package com.sequenceiq.freeipa.flow.freeipa.migration.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationFinalizeFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationCleanupRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationFinalizeFailedEvent;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@ExtendWith(MockitoExtension.class)
class MultiAzMigrationCleanupHandlerTest {

    private static final long STACK_ID = 1L;

    private static final Platform PLATFORM = Platform.platform("AZURE");

    private static final Variant VARIANT = Variant.variant("AZURE");

    private static final String RESOURCE_GROUP_NAME = "rg-name";

    @Mock
    private ResourceService resourceService;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @InjectMocks
    private MultiAzMigrationCleanupHandler underTest;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(MultiAzMigrationCleanupRequest.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Exception e = new Exception("test");

        Selectable result = underTest.defaultFailureEvent(STACK_ID, e, new Event<>(createRequest()));

        assertThat(result).isInstanceOf(MultiAzMigrationFinalizeFailedEvent.class);
        MultiAzMigrationFinalizeFailedEvent failure = (MultiAzMigrationFinalizeFailedEvent) result;
        assertEquals(STACK_ID, failure.getResourceId());
        assertEquals(e, failure.getException());
    }

    @Test
    void testDoAcceptSkipsCleanupWhenNoAvailabilitySetsFound() {
        when(resourceService.findAllByResourceStatusAndResourceTypeAndStackId(CommonStatus.CREATED, ResourceType.AZURE_AVAILABILITY_SET, STACK_ID))
                .thenReturn(List.of());

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(createRequest())));

        assertResultIsCleanupFinishedEvent(result);
        verify(cloudPlatformConnectors, never()).get(any(Platform.class), any(Variant.class));
        verify(azureUtils, never()).deleteAvailabilitySets(any(), any(), any());
        verify(resourceService, never()).deleteAll(any());
    }

    @Test
    void testDoAcceptDeletesAvailabilitySetsWhenPresent() {
        MultiAzMigrationCleanupRequest request = createRequest();
        when(request.getCloudContext().getPlatform()).thenReturn(PLATFORM);
        when(request.getCloudContext().getVariant()).thenReturn(VARIANT);
        List<Resource> availabilitySets = List.of(
                new Resource(ResourceType.AZURE_AVAILABILITY_SET, "as-1", null, null),
                new Resource(ResourceType.AZURE_AVAILABILITY_SET, "as-2", null, null));
        AzureClient azureClient = mock(AzureClient.class);
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(request.getCloudContext(), request.getCloudCredential());
        authenticatedContext.putParameter(AzureClient.class, azureClient);
        CloudConnector cloudConnector = mock(CloudConnector.class);
        Authenticator authenticator = mock(Authenticator.class);
        when(resourceService.findAllByResourceStatusAndResourceTypeAndStackId(CommonStatus.CREATED, ResourceType.AZURE_AVAILABILITY_SET, STACK_ID))
                .thenReturn(availabilitySets);
        when(cloudPlatformConnectors.get(PLATFORM, VARIANT)).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(request.getCloudContext(), request.getCloudCredential())).thenReturn(authenticatedContext);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(request.getCloudContext(), request.getCloudStack()))
                .thenReturn(RESOURCE_GROUP_NAME);

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(request)));

        assertResultIsCleanupFinishedEvent(result);
        verify(azureUtils).deleteAvailabilitySets(azureClient, RESOURCE_GROUP_NAME, List.of("as-1", "as-2"));
        verify(resourceService).deleteAll(availabilitySets);
    }

    private void assertResultIsCleanupFinishedEvent(Selectable result) {
        assertThat(result).isInstanceOf(StackEvent.class);
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(MultiAzMigrationFinalizeFlowEvent.MULTI_AZ_MIGRATION_CLEANUP_FINISHED_EVENT.event(), result.selector());
    }

    private MultiAzMigrationCleanupRequest createRequest() {
        return new MultiAzMigrationCleanupRequest(STACK_ID, mock(CloudContext.class), mock(CloudCredential.class), mock(CloudStack.class));
    }
}
