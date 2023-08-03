package com.sequenceiq.cloudbreak.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackImageFallbackResult;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackResult;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class UpscaleStackHandlerTest {

    @InjectMocks
    private UpscaleStackHandler underTest;

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
    private StackUpscaleService stackUpscaleService;

    @Mock
    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant("AZURE", null);
        when(context.getPlatformVariant()).thenReturn(cloudPlatformVariant);

        when(cloudPlatformConnectors.get(eq(cloudPlatformVariant))).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(eq(context), eq(cred))).thenReturn(authContext);

        AdjustmentTypeWithThreshold threshold = new AdjustmentTypeWithThreshold(AdjustmentType.BEST_EFFORT, 1L);
        request = new UpscaleStackRequest<>(context, cred, stack, List.of(), threshold, false);
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
        when(stackUpscaleService.upscale(eq(authContext), eq(request), eq(cloudConnector))).thenReturn(List.of(cloudResourceStatus));

        underTest.accept(new Event<>(request));

        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(UpscaleStackResult.class.getSimpleName().toUpperCase()), eventArgumentCaptor.capture());
        Event actualEvent = eventArgumentCaptor.getValue();
        assertNotNull(actualEvent);
        assertEquals(ResourceStatus.UPDATED, ((UpscaleStackResult) actualEvent.getData()).getResourceStatus());
    }

    @Test
    void testAcceptImageFallback() throws Exception {
        when(stackUpscaleService.upscale(eq(authContext), eq(request), eq(cloudConnector))).thenThrow(new CloudImageException("Could not sign image"));

        underTest.accept(new Event<>(request));

        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(UpscaleStackImageFallbackResult.class.getSimpleName().toUpperCase()), eventArgumentCaptor.capture());
        Event actualEvent = eventArgumentCaptor.getValue();
        assertNotNull(actualEvent);
        assertEquals(ResourceStatus.FAILED, ((UpscaleStackImageFallbackResult) actualEvent.getData()).getResourceStatus());
    }

    @Test
    void testAcceptQuotaException() throws Exception {
        when(stackUpscaleService.upscale(eq(authContext), eq(request), eq(cloudConnector))).thenThrow(new QuotaExceededException(1, 1, 1, "", null));

        underTest.accept(new Event<>(request));

        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(UpscaleStackResult.class.getSimpleName().toUpperCase() + "_ERROR"), eventArgumentCaptor.capture());
        Event actualEvent = eventArgumentCaptor.getValue();
        assertNotNull(actualEvent);
        assertEquals(ResourceStatus.FAILED, ((UpscaleStackResult) actualEvent.getData()).getResourceStatus());
    }
}