package com.sequenceiq.freeipa.flow.freeipa.upscale.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudImageException;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackImageFallbackResult;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackRequest;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackResult;

@ExtendWith(MockitoExtension.class)
class FreeipaUpscaleStackHandlerTest {

    @InjectMocks
    private FreeipaUpscaleStackHandler underTest;

    private UpscaleStackRequest<UpscaleStackResult> request;

    @Mock
    private CloudContext context;

    @Mock
    private CloudCredential cred;

    @Mock
    private CloudStack stack;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatedContext authContext;

    @Mock
    private EventBus eventBus;

    @Mock
    private ResourceConnector resourceConnector;

    @BeforeEach
    void setUp() {
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant("AZURE", null);
        when(context.getPlatformVariant()).thenReturn(cloudPlatformVariant);

        when(cloudPlatformConnectors.get(eq(cloudPlatformVariant))).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(eq(context), eq(cred))).thenReturn(authContext);
        when(cloudConnector.resources()).thenReturn(resourceConnector);

        AdjustmentTypeWithThreshold threshold = new AdjustmentTypeWithThreshold(AdjustmentType.BEST_EFFORT, 1L);
        request = new UpscaleStackRequest<>(context, cred, stack, List.of(), threshold);
    }

    @Test
    void testAccept() throws Exception {
        CloudResource cloudResource = CloudResource.builder()
                .withType(ResourceType.AZURE_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withName("c001")
                .withParameters(Map.of())
                .build();
        CloudResourceStatus cloudResourceStatus = new CloudResourceStatus(cloudResource, ResourceStatus.CREATED);
        when(resourceConnector.upscale(eq(authContext), eq(stack), eq(List.of()), eq(request.getAdjustmentWithThreshold())))
                .thenReturn(List.of(cloudResourceStatus));

        underTest.accept(new Event<>(request));

        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(UpscaleStackResult.class.getSimpleName().toUpperCase(Locale.ROOT)), eventArgumentCaptor.capture());
        Event actualEvent = eventArgumentCaptor.getValue();
        assertNotNull(actualEvent);
        assertEquals(ResourceStatus.UPDATED, ((UpscaleStackResult) actualEvent.getData()).getResourceStatus());
    }

    @Test
    void testAcceptImageFallback() throws Exception {
        when(resourceConnector.upscale(eq(authContext), eq(stack), eq(List.of()), eq(request.getAdjustmentWithThreshold())))
                .thenThrow(new CloudImageException("Could not sign image"));

        underTest.accept(new Event<>(request));

        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(UpscaleStackImageFallbackResult.class.getSimpleName().toUpperCase(Locale.ROOT)), eventArgumentCaptor.capture());
        Event actualEvent = eventArgumentCaptor.getValue();
        assertNotNull(actualEvent);
        assertEquals(ResourceStatus.FAILED, ((UpscaleStackImageFallbackResult) actualEvent.getData()).getResourceStatus());
    }

    @Test
    void testAcceptQuotaException() throws Exception {
        when(resourceConnector.upscale(eq(authContext), eq(stack), eq(List.of()), eq(request.getAdjustmentWithThreshold())))
                .thenThrow(new QuotaExceededException(1, 1, 1, "", null));

        underTest.accept(new Event<>(request));

        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(UpscaleStackResult.class.getSimpleName().toUpperCase(Locale.ROOT) + "_ERROR"), eventArgumentCaptor.capture());
        Event actualEvent = eventArgumentCaptor.getValue();
        assertNotNull(actualEvent);
        assertEquals(ResourceStatus.FAILED, ((UpscaleStackResult) actualEvent.getData()).getResourceStatus());
    }
}