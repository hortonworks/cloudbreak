package com.sequenceiq.freeipa.flow.stack.migration.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsAuthenticator;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsTerminateService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.DeleteCloudFormationRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.DeleteCloudFormationResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class DeleteCloudFormationHandlerTest {

    private static final long RESOUCE_ID = 123L;

    @InjectMocks
    private DeleteCloudFormationHandler underTest;

    @Mock
    private EventBus eventBus;

    @Mock
    private AwsTerminateService awsTerminateService;

    @Mock
    private AwsAuthenticator awsAuthenticator;

    @Mock
    private ResourceRetriever resourceRetriever;

    @Mock
    private ResourceNotifier resourceNotifier;

    @Mock
    private AwsMigrationUtil awsMigrationUtil;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private AuthenticatedContext ac;

    @Test
    public void testAcceptWhenCfCloudResourceNotPresent() {
        DeleteCloudFormationRequest request = new DeleteCloudFormationRequest(cloudContext, cloudCredential, cloudStack);
        Event<DeleteCloudFormationRequest> event = new Event<>(request);

        when(cloudContext.getId()).thenReturn(RESOUCE_ID);
        when(resourceRetriever.findByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.CLOUDFORMATION_STACK, RESOUCE_ID)).thenReturn(Optional.empty());

        ArgumentCaptor<Event<DeleteCloudFormationResult>> resultCaptor = ArgumentCaptor.forClass(Event.class);
        underTest.accept(event);
        verify(eventBus).notify(eq("DELETECLOUDFORMATIONRESULT"), resultCaptor.capture());
        Event<DeleteCloudFormationResult> result = resultCaptor.getValue();
        verify(awsMigrationUtil, never()).allInstancesDeletedFromCloudFormation(any(), any());

        assertTrue(result.getData().isCloudFormationTemplateDeleted());
    }

    @Test
    public void testAcceptWhenCfCloudResourcePresentAndAllInstanceDeleted() {
        DeleteCloudFormationRequest request = new DeleteCloudFormationRequest(cloudContext, cloudCredential, cloudStack);
        Event<DeleteCloudFormationRequest> event = new Event<>(request);
        CloudResource cloudResource = mock(CloudResource.class);

        when(cloudContext.getId()).thenReturn(RESOUCE_ID);
        when(awsAuthenticator.authenticate(cloudContext, cloudCredential)).thenReturn(ac);
        when(awsMigrationUtil.allInstancesDeletedFromCloudFormation(ac, cloudResource)).thenReturn(true);
        when(resourceRetriever.findByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.CLOUDFORMATION_STACK, RESOUCE_ID))
                .thenReturn(Optional.of(cloudResource));

        ArgumentCaptor<Event<DeleteCloudFormationResult>> resultCaptor = ArgumentCaptor.forClass(Event.class);
        underTest.accept(event);
        verify(eventBus).notify(eq("DELETECLOUDFORMATIONRESULT"), resultCaptor.capture());
        Event<DeleteCloudFormationResult> result = resultCaptor.getValue();
        verify(awsMigrationUtil).allInstancesDeletedFromCloudFormation(any(), any());
        verify(awsTerminateService).terminate(ac, cloudStack, List.of(cloudResource));
        verify(resourceNotifier).notifyDeletion(cloudResource, cloudContext);

        assertTrue(result.getData().isCloudFormationTemplateDeleted());
    }

    @Test
    public void testAcceptWhenCfCloudResourcePresentButInstancesNotDeleted() {
        DeleteCloudFormationRequest request = new DeleteCloudFormationRequest(cloudContext, cloudCredential, cloudStack);
        Event<DeleteCloudFormationRequest> event = new Event<>(request);
        CloudResource cloudResource = mock(CloudResource.class);

        when(cloudContext.getId()).thenReturn(RESOUCE_ID);
        when(awsAuthenticator.authenticate(cloudContext, cloudCredential)).thenReturn(ac);
        when(awsMigrationUtil.allInstancesDeletedFromCloudFormation(ac, cloudResource)).thenReturn(false);
        when(resourceRetriever.findByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.CLOUDFORMATION_STACK, RESOUCE_ID))
                .thenReturn(Optional.of(cloudResource));

        ArgumentCaptor<Event<DeleteCloudFormationResult>> resultCaptor = ArgumentCaptor.forClass(Event.class);
        underTest.accept(event);
        verify(eventBus).notify(eq("DELETECLOUDFORMATIONRESULT"), resultCaptor.capture());
        Event<DeleteCloudFormationResult> result = resultCaptor.getValue();
        verify(awsMigrationUtil).allInstancesDeletedFromCloudFormation(any(), any());
        verify(awsTerminateService, never()).terminate(ac, cloudStack, List.of(cloudResource));
        verify(resourceNotifier, never()).notifyDeletion(cloudResource, cloudContext);

        assertFalse(result.getData().isCloudFormationTemplateDeleted());
    }

    @Test
    public void testAcceptWhenExceptionOccurred() {
        DeleteCloudFormationRequest request = new DeleteCloudFormationRequest(cloudContext, cloudCredential, cloudStack);
        Event<DeleteCloudFormationRequest> event = new Event<>(request);

        when(cloudContext.getId()).thenReturn(RESOUCE_ID);
        RuntimeException exception = new RuntimeException("CF delete issue");
        doThrow(exception).when(awsAuthenticator).authenticate(cloudContext, cloudCredential);

        ArgumentCaptor<Event<DeleteCloudFormationResult>> resultCaptor = ArgumentCaptor.forClass(Event.class);
        underTest.accept(event);
        verify(eventBus).notify(eq("AWSVARIANTMIGRATIONFAILEDEVENT"), resultCaptor.capture());
        Event<DeleteCloudFormationResult> result = resultCaptor.getValue();

        assertEquals(exception, result.getData().getErrorDetails());
        assertEquals(EventStatus.FAILED, result.getData().getStatus());
        assertEquals("CF delete issue", result.getData().getStatusReason());
    }
}
