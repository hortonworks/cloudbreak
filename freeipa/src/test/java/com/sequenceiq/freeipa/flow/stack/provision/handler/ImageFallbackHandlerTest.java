package com.sequenceiq.freeipa.flow.stack.provision.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackFailed;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackRequest;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackSuccess;
import com.sequenceiq.freeipa.service.image.FreeIpaImageFilterSettings;
import com.sequenceiq.freeipa.service.image.ImageFallbackService;
import com.sequenceiq.freeipa.service.image.ImageProviderFactory;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class ImageFallbackHandlerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @Mock
    private StackService stackService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ImageService imageService;

    @Mock
    private ImageProviderFactory imageProviderFactory;

    @Mock
    private AzureImageFormatValidator azureImageFormatValidator;

    @Mock
    private ImageFallbackService imageFallbackService;

    @InjectMocks
    private ImageFallbackHandler imageFallbackHandler;

    @Test
    public void testDoAcceptForNonAzurePlatform() {
        HandlerEvent<ImageFallbackRequest> event = mock(HandlerEvent.class);
        ImageFallbackRequest request = mock(ImageFallbackRequest.class);
        Stack stack = mock(Stack.class);

        when(event.getData()).thenReturn(request);
        when(request.getResourceId()).thenReturn(123L);
        when(stackService.getStackById(123L)).thenReturn(stack);
        when(stack.getCloudPlatform()).thenReturn("AWS");

        ImageEntity currentImage = mock(ImageEntity.class);
        when(currentImage.getImageName()).thenReturn("cloudera:image");
        when(imageService.getByStack(stack)).thenReturn(currentImage);

        Selectable result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> imageFallbackHandler.doAccept(event));

        assertTrue(result instanceof ImageFallbackFailed);
        assertEquals("Failed to start instances with the designated image: cloudera:image. Image fallback is only supported on the Azure cloud platform",
                result.getException().getMessage());
    }

    @Test
    public void testDoAcceptForAzureOnlyMarketplaceImagesEnabled() {
        HandlerEvent<ImageFallbackRequest> event = mock(HandlerEvent.class);
        ImageFallbackRequest request = mock(ImageFallbackRequest.class);
        Stack stack = mock(Stack.class);
        ImageEntity currentImage = mock(ImageEntity.class);

        when(event.getData()).thenReturn(request);
        when(request.getResourceId()).thenReturn(123L);
        when(stackService.getStackById(123L)).thenReturn(stack);
        when(stack.getCloudPlatform()).thenReturn("AZURE");
        when(entitlementService.azureOnlyMarketplaceImagesEnabled(anyString())).thenReturn(true);
        when(imageService.getByStack(stack)).thenReturn(currentImage);
        when(currentImage.getImageName()).thenReturn("currentImage");

        Selectable result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> imageFallbackHandler.doAccept(event));

        assertTrue(result instanceof ImageFallbackFailed);
        assertEquals("Azure Marketplace image terms were not accepted, cannot start instances with image: currentImage. Fallback to VHD image is not possible,"
                        + " only Azure Marketplace images allowed. Please accept image terms or turn on automatic image terms acceptance.",
                result.getException().getMessage());
    }

    @Test
    public void testDoAcceptSuccessfulFallback() {
        HandlerEvent<ImageFallbackRequest> event = mock(HandlerEvent.class);
        ImageFallbackRequest request = mock(ImageFallbackRequest.class);
        Stack stack = mock(Stack.class);
        ImageEntity currentImage = mock(ImageEntity.class);
        FreeIpaImageFilterSettings settings =
                new FreeIpaImageFilterSettings(null, null, null, null, null, "azure", false);

        when(event.getData()).thenReturn(request);
        when(request.getResourceId()).thenReturn(123L);
        when(stackService.getStackById(123L)).thenReturn(stack);
        when(stack.getCloudPlatform()).thenReturn("AZURE");
        when(imageService.getByStack(stack)).thenReturn(currentImage);

        Selectable result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> imageFallbackHandler.doAccept(event));

        assertTrue(result instanceof ImageFallbackSuccess);
        assertEquals(123L, result.getResourceId());
    }

}