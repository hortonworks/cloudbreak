package com.sequenceiq.freeipa.flow.stack.provision.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

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
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackFailed;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackRequest;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackSuccess;
import com.sequenceiq.freeipa.service.image.FreeIpaImageFilterSettings;
import com.sequenceiq.freeipa.service.image.ImageProvider;
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
                ((ImageFallbackFailed) result).getException().getMessage());
    }

    @Test
    public void testDoAcceptForRedhat8VhdImage() {
        HandlerEvent<ImageFallbackRequest> event = mock(HandlerEvent.class);
        ImageFallbackRequest request = mock(ImageFallbackRequest.class);
        Stack stack = mock(Stack.class);
        ImageEntity currentImage = mock(ImageEntity.class);

        when(event.getData()).thenReturn(request);
        when(request.getResourceId()).thenReturn(123L);
        when(stackService.getStackById(123L)).thenReturn(stack);
        when(stack.getCloudPlatform()).thenReturn("AZURE");
        when(imageService.getByStack(stack)).thenReturn(currentImage);
        when(currentImage.getOsType()).thenReturn("redhat8");
        when(currentImage.getImageName()).thenReturn("http://redhat8.vhd");

        when(azureImageFormatValidator.isVhdImageFormat(anyString())).thenReturn(true);

        Selectable result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> imageFallbackHandler.doAccept(event));

        assertTrue(result instanceof ImageFallbackFailed);
        assertEquals("Failed to start instances with image: http://redhat8.vhd. No valid fallback path from Redhat 8 VHD image.",
                ((ImageFallbackFailed) result).getException().getMessage());
    }

    @Test
    public void testDoAcceptSuccessfulFallback() throws Exception {
        HandlerEvent<ImageFallbackRequest> event = mock(HandlerEvent.class);
        ImageFallbackRequest request = mock(ImageFallbackRequest.class);
        Stack stack = mock(Stack.class);
        ImageEntity currentImage = mock(ImageEntity.class);
        ImageProvider imageProvider = mock(ImageProvider.class);
        ImageWrapper imageWrapper = ImageWrapper.ofCoreImage(mock(Image.class), "catalogName");
        FreeIpaImageFilterSettings settings =
                new FreeIpaImageFilterSettings(null, null, null, null, null, "azure", false);

        when(event.getData()).thenReturn(request);
        when(request.getResourceId()).thenReturn(123L);
        when(stackService.getStackById(123L)).thenReturn(stack);
        when(stack.getCloudPlatform()).thenReturn("AZURE");
        when(imageService.getByStack(stack)).thenReturn(currentImage);
        when(imageProviderFactory.getImageProvider(any())).thenReturn(imageProvider);
        when(imageProvider.getImage(settings)).thenReturn(Optional.of(imageWrapper));

        Selectable result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> imageFallbackHandler.doAccept(event));

        assertTrue(result instanceof ImageFallbackSuccess);
        assertEquals(123L, ((ImageFallbackSuccess) result).getResourceId());
    }

}