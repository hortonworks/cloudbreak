package com.sequenceiq.cloudbreak.reactor.handler;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class ImageFallbackServiceTest {

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private ImageService imageService;

    @Mock
    private UserDataService userDataService;

    @Mock
    private PlatformStringTransformer platformStringTransformer;

    @Mock
    private AzureImageFormatValidator azureImageFormatValidator;

    @InjectMocks
    private ImageFallbackService imageFallbackService;

    @Test
    public void testFallbackToVhdForNonAzurePlatform() throws Exception {
        Long stackId = 123L;
        StackView stackView = mock(StackView.class);

        when(stackDtoService.getStackViewById(stackId)).thenReturn(stackView);
        when(stackView.getCloudPlatform()).thenReturn("AWS");

        imageFallbackService.fallbackToVhd(stackId);

        verify(stackDtoService).getStackViewById(stackId);
        verifyNoMoreInteractions(stackDtoService);
    }

    @Test
    public void testFallbackToVhdForRedhat8VhdImage() throws Exception {
        Long stackId = 123L;
        StackView stackView = mock(StackView.class);
        com.sequenceiq.cloudbreak.domain.stack.Component component = mock(com.sequenceiq.cloudbreak.domain.stack.Component.class);
        Image currentImage =
                new Image(null, Map.of(), "redhat8", "redhat8", null, null, null, Map.of(), null, 0L);

        when(stackDtoService.getStackViewById(stackId)).thenReturn(stackView);
        when(stackView.getCloudPlatform()).thenReturn("AZURE");
        when(componentConfigProviderService.getImageComponent(stackId)).thenReturn(component);
        when(component.getAttributes()).thenReturn(new Json(currentImage));
        when(azureImageFormatValidator.isVhdImageFormat(eq(currentImage))).thenReturn(true);

        assertThrows(CloudbreakServiceException.class, () -> imageFallbackService.fallbackToVhd(stackId));

        verify(stackDtoService).getStackViewById(stackId);
        verify(componentConfigProviderService).getImageComponent(stackId);
    }
}