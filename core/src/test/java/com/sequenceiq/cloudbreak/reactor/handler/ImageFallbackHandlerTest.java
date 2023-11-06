package com.sequenceiq.cloudbreak.reactor.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ImageFallbackFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ImageFallbackRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ImageFallbackSuccess;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ImageFallbackHandlerTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private ImageFallbackHandler underTest;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ImageFallbackService imageFallbackService;

    @Test
    void testDoAcceptOnlyMarketplaceImagesAllowed() {
        HandlerEvent<ImageFallbackRequest> event = new HandlerEvent<>(new Event<>(new ImageFallbackRequest(STACK_ID, createCloudContext())));
        when(entitlementService.azureOnlyMarketplaceImagesEnabled(any())).thenReturn(true);

        Selectable result = ThreadBasedUserCrnProvider.doAs("crn:altus:iam:us-west-1:123:user:456", () -> underTest.doAccept(event));

        assertTrue(result instanceof ImageFallbackFailed);
        ImageFallbackFailed fallbackFailed = (ImageFallbackFailed) result;
        assertEquals("Cannot fallback to VHD image. Only Azure Marketplace images allowed.", fallbackFailed.getException().getMessage());
    }

    @Test
    void testDoAccept() throws Exception {
        HandlerEvent<ImageFallbackRequest> event = new HandlerEvent<>(new Event<>(new ImageFallbackRequest(STACK_ID, createCloudContext())));
        when(entitlementService.azureOnlyMarketplaceImagesEnabled(any())).thenReturn(false);

        Selectable result = ThreadBasedUserCrnProvider.doAs("crn:altus:iam:us-west-1:123:user:456", () -> underTest.doAccept(event));

        assertTrue(result instanceof ImageFallbackSuccess);
        verify(imageFallbackService).fallbackToVhd(STACK_ID);
    }

    @Test
    void testDoAcceptException() throws Exception {
        HandlerEvent<ImageFallbackRequest> event = new HandlerEvent<>(new Event<>(new ImageFallbackRequest(STACK_ID, createCloudContext())));
        when(entitlementService.azureOnlyMarketplaceImagesEnabled(any())).thenReturn(false);
        CloudbreakServiceException exception = new CloudbreakServiceException("Image component error");
        doThrow(exception).when(imageFallbackService).fallbackToVhd(eq(STACK_ID));
        Selectable result = ThreadBasedUserCrnProvider.doAs("crn:altus:iam:us-west-1:123:user:456", () -> underTest.doAccept(event));

        assertTrue(result instanceof ImageFallbackFailed);
        assertEquals(exception, result.getException());
        verify(imageFallbackService).fallbackToVhd(STACK_ID);
    }

    private CloudContext createCloudContext() {
        return CloudContext.Builder.builder()
                .withId(STACK_ID)
                .withName("")
                .withCrn("")
                .withPlatform(Platform.platform("AZURE"))
                .withVariant("")
                .build();
    }
}