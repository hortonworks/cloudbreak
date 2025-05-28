package com.sequenceiq.freeipa.service.image;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@ExtendWith(MockitoExtension.class)
class ImageFallbackServiceTest {

    @Mock
    private ImageService imageService;

    @Mock
    private ImageProviderFactory imageProviderFactory;

    @Mock
    private AzureImageFormatValidator azureImageFormatValidator;

    @InjectMocks
    private ImageFallbackService underTest;

    @Test
    void testPerformImageFallbackShouldSetNewImageNameAndSaveImage() {
        // Given
        ImageEntity currentImage = new ImageEntity();
        currentImage.setImageName("currentImageName");
        Stack stack = new Stack();
        stack.setCloudPlatform("AZURE");
        stack.setRegion("region");
        stack.setAccountId("accountId");

        Image image = createImage();
        ImageWrapper imageWrapper = ImageWrapper.ofCoreImage(image, "catalogName");

        ImageProvider imageProvider = mock(ImageProvider.class);
        when(imageProviderFactory.getImageProvider(any())).thenReturn(imageProvider);
        when(imageProvider.getImage(any())).thenReturn(Optional.of(imageWrapper));
        when(imageService.determineImageNameByRegion(any(), any(), any())).thenReturn("newImageName");
        when(azureImageFormatValidator.isMarketplaceImageFormat(anyString())).thenReturn(true);

        // When
        underTest.performImageFallback(currentImage, stack);

        ArgumentCaptor<ImageEntity> captor = ArgumentCaptor.forClass(ImageEntity.class);
        verify(imageService).save(captor.capture());

        // Then
        ImageEntity newImage = captor.getValue();
        assertEquals("newImageName", newImage.getImageName());
        assertEquals("accountId", newImage.getAccountId());
    }

    @Test
    void testPerformImageFallbackShouldNotRunForInvalidProvider() {
        // Given
        ImageEntity currentImage = new ImageEntity();
        currentImage.setImageName("currentImageName");
        Stack stack = new Stack();
        stack.setCloudPlatform("otherProvider");
        stack.setRegion("region");
        stack.setAccountId("accountId");

        // When
        underTest.performImageFallback(currentImage, stack);

        // Then
        verifyNoInteractions(imageService);
    }

    @Test
    void testPerformImageFallbackShouldNotRunForNonMarketplaceImage() {
        // Given
        ImageEntity currentImage = new ImageEntity();
        currentImage.setImageName("currentImageName");
        Stack stack = new Stack();
        stack.setCloudPlatform("AZURE");
        stack.setRegion("region");
        stack.setAccountId("accountId");

        when(azureImageFormatValidator.isMarketplaceImageFormat(anyString())).thenReturn(false);

        // When
        underTest.performImageFallback(currentImage, stack);

        // Then
        verifyNoInteractions(imageService);
    }

    @Test
    void testGetImageWrapperShouldThrowExceptionOnNoValidFallbackPath() {
        // Given
        ImageEntity currentImage = new ImageEntity();
        currentImage.setOsType("redhat8");
        currentImage.setImageName("vhdImageName");
        currentImage.setSourceImage("redhat8:source:image");

        Stack stack = new Stack();
        stack.setCloudPlatform("cloudPlatform");
        stack.setRegion("region");

        when(azureImageFormatValidator.isVhdImageFormat(anyString())).thenReturn(true);

        // When / Then
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.getImageWrapper(currentImage, stack));

        assertEquals("Failed to start instances with image: vhdImageName. The current image is a Redhat 8 VHD image, " +
                "please check if the source image is signed: redhat8:source:image.", exception.getMessage());
    }

    @Test
    void testGetImageWrapperShouldThrowExceptionOnNoVhdImageFound() {
        // Given
        ImageEntity currentImage = new ImageEntity();
        currentImage.setOsType("someOtherOs");
        currentImage.setImageId("imageId");
        currentImage.setImageCatalogUrl("catalogUrl");
        Stack stack = new Stack();
        stack.setCloudPlatform("cloudPlatform");
        stack.setRegion("region");

        when(imageProviderFactory.getImageProvider(any())).thenReturn(mock(ImageProvider.class));
        when(imageProviderFactory.getImageProvider(any()).getImage(any())).thenReturn(Optional.empty());

        // When / Then
        ImageNotFoundException exception = assertThrows(ImageNotFoundException.class,
                () -> underTest.getImageWrapper(currentImage, stack));

        assertEquals("Azure Marketplace image terms were not accepted. Attempted to fallback to VHD image, but failed. "
                + "No VHD image found for image id imageId and region region.", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("provideData")
    void testDetermineFallbackImageIfPermitted(boolean marketplaceFormat, String platform, boolean permitted) {
        StackContext context = mock(StackContext.class);
        Stack stack = mock(Stack.class);
        CloudContext cloudContext = mock(CloudContext.class);
        ImageEntity imageEntity = mock(ImageEntity.class);
        Image image = mock(Image.class);

        when(stack.getCloudPlatform()).thenReturn(platform);
        when(imageEntity.getImageName()).thenReturn("imageName");
        when(imageService.getByStack(any())).thenReturn(imageEntity);
        when(context.getStack()).thenReturn(stack);
        when(context.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(Location.location(region("us-west-1")));
        when(cloudContext.getPlatform()).thenReturn(platform(platform));
        lenient().when(imageService.getImageForStack(stack)).thenReturn(image);
        lenient().when(imageService.determineImageNameByRegion(anyString(), anyString(), any())).thenReturn("fallback-image-name");
        lenient().when(azureImageFormatValidator.isMarketplaceImageFormat(anyString())).thenReturn(marketplaceFormat);

        Optional<String> result = underTest.determineFallbackImageIfPermitted(context);
        if (permitted) {
            Assertions.assertTrue(result.isPresent());
            assertEquals("fallback-image-name", result.get());
        } else {
            Assertions.assertTrue(result.isEmpty());
        }
    }

    private static Stream<Arguments> provideData() {
        return Stream.of(
                // imageMarketplaceFormat, platform, permitted
                Arguments.of(false, "AZURE", false),
                Arguments.of(true, "AZURE", true),
                Arguments.of(true, "AWS", false)
                );
    }

    private Image createImage() {
        return new Image(123L, "date", "desc", "linux", "1234-456", Map.of(), "magicOs", Map.of(), false, "x86_64");
    }
}