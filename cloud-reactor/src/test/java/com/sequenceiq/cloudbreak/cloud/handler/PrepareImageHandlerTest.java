package com.sequenceiq.cloudbreak.cloud.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageFallbackRequiredResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageResult;
import com.sequenceiq.cloudbreak.cloud.exception.CloudImageFallbackException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.PrepareImageType;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.eventbus.Promise;

@ExtendWith(MockitoExtension.class)
class PrepareImageHandlerTest {

    private static final Long RESOURCE_ID = 0L;

    @InjectMocks
    private PrepareImageHandler handler;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private EventBus eventBus;

    @Mock
    private Event<PrepareImageRequest> event;

    @Mock
    private PrepareImageRequest<PrepareImageResult> request;

    @Mock
    private CloudConnector connector;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private Image image;

    @Mock
    private CloudStack stack;

    @Mock
    private PrepareImageResult prepareImageResult;

    @Mock
    private PrepareImageFallbackRequiredResult fallbackResult;

    @Mock
    private Authenticator authenticator;

    @Mock
    private Setup setup;

    @Test
    void testAcceptSuccessfulImagePreparation() {
        when(event.getData()).thenReturn(request);
        when(request.getCloudContext()).thenReturn(mock(CloudContext.class));
        when(request.getCloudCredential()).thenReturn(mock(CloudCredential.class));
        when(request.getImage()).thenReturn(image);
        when(request.getStack()).thenReturn(stack);
        when(request.getPrepareImageType()).thenReturn(mock(PrepareImageType.class));
        when(request.getImageFallbackTarget()).thenReturn(null);
        when(request.getResourceId()).thenReturn(RESOURCE_ID);
        Promise promise = mock(Promise.class);
        when(request.getResult()).thenReturn(promise);
        when(cloudPlatformConnectors.get(any())).thenReturn(connector);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(any(), any())).thenReturn(authenticatedContext);
        when(connector.setup()).thenReturn(setup);

        handler.accept(event);

        verify(setup).prepareImage(authenticatedContext, stack, image, request.getPrepareImageType(), null);
        verify(eventBus).notify(anyString(), any(Event.class));
    }

    @Test
    void testAcceptFallbackImagePreparation() {
        when(event.getData()).thenReturn(request);
        when(request.getCloudContext()).thenReturn(mock(CloudContext.class));
        when(request.getCloudCredential()).thenReturn(mock(CloudCredential.class));
        when(request.getImage()).thenReturn(image);
        when(request.getStack()).thenReturn(stack);
        when(request.getPrepareImageType()).thenReturn(mock(PrepareImageType.class));
        when(request.getImageFallbackTarget()).thenReturn("fallback-target");
        when(request.getResourceId()).thenReturn(RESOURCE_ID);
        Promise promise = mock(Promise.class);
        when(request.getResult()).thenReturn(promise);
        when(cloudPlatformConnectors.get(any())).thenReturn(connector);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(any(), any())).thenReturn(authenticatedContext);
        when(connector.setup()).thenReturn(setup);
        doThrow(new CloudImageFallbackException("Fallback required")).when(setup).prepareImage(any(), any(), any(), any(), any());

        handler.accept(event);

        verify(eventBus).notify(anyString(), any(Event.class));
    }

    @Test
    void testAcceptImagePreparationFailure() {
        when(event.getData()).thenReturn(request);
        when(request.getCloudContext()).thenReturn(mock(CloudContext.class));
        when(request.getCloudCredential()).thenReturn(mock(CloudCredential.class));
        when(request.getImage()).thenReturn(image);
        when(request.getStack()).thenReturn(stack);
        when(request.getPrepareImageType()).thenReturn(mock(PrepareImageType.class));
        when(request.getImageFallbackTarget()).thenReturn(null);
        when(request.getResourceId()).thenReturn(RESOURCE_ID);
        Promise promise = mock(Promise.class);
        when(request.getResult()).thenReturn(promise);
        when(cloudPlatformConnectors.get(any())).thenReturn(connector);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(any(), any())).thenReturn(authenticatedContext);
        when(connector.setup()).thenReturn(setup);
        doThrow(new RuntimeException("General failure")).when(setup).prepareImage(any(), any(), any(), any(), any());

        handler.accept(event);

        verify(eventBus).notify(anyString(), any(Event.class));
    }
}