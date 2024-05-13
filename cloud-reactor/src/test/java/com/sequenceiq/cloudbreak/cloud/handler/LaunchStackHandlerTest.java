package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudImageException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class LaunchStackHandlerTest {

    @InjectMocks
    private LaunchStackHandler underTest;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private Authenticator authentication;

    @Mock
    private AuthenticatedContext authenticatedCtx;

    @Mock
    private ResourceConnector resourceConnector;

    @Mock
    private PersistenceNotifier persistanceNotifier;

    @Mock
    private PollTaskFactory statusCheckFactory;

    @Mock
    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        when(cloudConnector.authentication()).thenReturn(authentication);
        when(authentication.authenticate(any(), any())).thenReturn(authenticatedCtx);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
    }

    @Test
    void testAcceptWithResourceCreated() throws Exception {
        CloudContext cloudCtx = mock(CloudContext.class);
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(CloudPlatform.AZURE.name(), CloudPlatform.AZURE.name());
        when(cloudCtx.getPlatformVariant()).thenReturn(cloudPlatformVariant);

        CloudStack cloudStack = mock(CloudStack.class);
        CloudCredential cloudCredential = new CloudCredential();

        LaunchStackRequest data = new LaunchStackRequest(cloudCtx, cloudCredential, cloudStack, AdjustmentType.EXACT, 0L);
        Event<LaunchStackRequest> request = new Event<>(data);

        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        CloudResource cloudResource = CloudResource.builder()
                .withType(ResourceType.AZURE_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withParameters(Map.of())
                .withName("azure-vm")
                .build();
        when(resourceConnector.launch(authenticatedCtx, cloudStack, persistanceNotifier, data.getAdjustmentWithThreshold()))
                .thenReturn(List.of(new CloudResourceStatus(cloudResource, ResourceStatus.CREATED)));

        PollTask task = mock(PollTask.class);
        when(statusCheckFactory.newPollResourcesStateTask(eq(authenticatedCtx), any(List.class), eq(true)))
                .thenReturn(task);
        when(task.completed(any())).thenReturn(true);

        underTest.accept(request);

        verify(authentication).authenticate(cloudCtx, cloudCredential);
        verify(resourceConnector).launch(authenticatedCtx, cloudStack, persistanceNotifier, data.getAdjustmentWithThreshold());

        ArgumentCaptor<Event> resultArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(LaunchStackResult.class.getSimpleName().toUpperCase(Locale.ROOT)), resultArgumentCaptor.capture());

        Event<LaunchStackResult> resultEvent = resultArgumentCaptor.getValue();
        LaunchStackResult result = resultEvent.getData();
        assertFalse(result.getResults().isEmpty());
        assertEquals(1, result.getResults().size());
        assertEquals(cloudResource.getName(), result.getResults().get(0).getCloudResource().getName());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testAcceptWithAzureImageFailure(boolean hasFallbackImage) throws Exception {
        CloudContext cloudCtx = mock(CloudContext.class);
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(CloudPlatform.AZURE.name(), CloudPlatform.AZURE.name());
        when(cloudCtx.getPlatformVariant()).thenReturn(cloudPlatformVariant);

        CloudStack cloudStack = mock(CloudStack.class);
        CloudCredential cloudCredential = new CloudCredential();

        LaunchStackRequest data = new LaunchStackRequest(cloudCtx, cloudCredential, cloudStack, AdjustmentType.EXACT, 0L,
                hasFallbackImage ? Optional.of("image") : Optional.empty());
        Event<LaunchStackRequest> request = new Event<>(data);

        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(resourceConnector.launch(authenticatedCtx, cloudStack, persistanceNotifier, data.getAdjustmentWithThreshold()))
                .thenThrow(new CloudImageException("Marketplace image not signed"));

        underTest.accept(request);

        verify(authentication).authenticate(cloudCtx, cloudCredential);
        verify(resourceConnector).launch(authenticatedCtx, cloudStack, persistanceNotifier, data.getAdjustmentWithThreshold());
        if (hasFallbackImage) {
            verify(eventBus).notify(eq("IMAGEFALLBACK"), any(Event.class));
        } else {
            verify(eventBus).notify(eq("LAUNCHSTACKRESULT_ERROR"), any(Event.class));
        }
    }

    @Test
    void testAcceptWithCloudFailure() throws Exception {
        CloudContext cloudCtx = mock(CloudContext.class);
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(CloudPlatform.AZURE.name(), CloudPlatform.AZURE.name());
        when(cloudCtx.getPlatformVariant()).thenReturn(cloudPlatformVariant);

        CloudStack cloudStack = mock(CloudStack.class);
        CloudCredential cloudCredential = new CloudCredential();

        LaunchStackRequest data = new LaunchStackRequest(cloudCtx, cloudCredential, cloudStack, AdjustmentType.EXACT, 0L);
        Event<LaunchStackRequest> request = new Event<>(data);

        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(resourceConnector.launch(authenticatedCtx, cloudStack, persistanceNotifier, data.getAdjustmentWithThreshold()))
                .thenThrow(new CloudConnectorException("Provision failed"));

        underTest.accept(request);

        verify(authentication).authenticate(cloudCtx, cloudCredential);
        verify(resourceConnector).launch(authenticatedCtx, cloudStack, persistanceNotifier, data.getAdjustmentWithThreshold());
        String failureSelector = LaunchStackResult.class.getSimpleName().toUpperCase(Locale.ROOT) + "_ERROR";
        verify(eventBus).notify(eq(failureSelector), any(Event.class));
    }
}