package com.sequenceiq.freeipa.service.image;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.azure.image.AzureImageFilter;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.converter.image.CoreImageToImageConverter;
import com.sequenceiq.freeipa.converter.image.ImageToCoreImageConverter;

@ExtendWith(MockitoExtension.class)
class ProviderSpecificImageFilterTest {

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CoreImageToImageConverter coreImageToImageConverter;

    @Mock
    private ImageToCoreImageConverter imageToCoreImageConverter;

    @InjectMocks
    private ProviderSpecificImageFilter underTest;

    @Test
    public void testForAValidProvider() {
        CloudConnector connector = mock(CloudConnector.class);
        PlatformParameters platformParameters = mock(PlatformParameters.class);
        AzureImageFilter imageFilter = mock(AzureImageFilter.class);
        Image image = mock(Image.class);
        Image imageToBeFiltered = mock(Image.class);
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image coreImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image coreImageToBeFiltered = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);

        List<Image> imageList = List.of(image, imageToBeFiltered);
        List<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> coreImageList = List.of(coreImage, coreImageToBeFiltered);
        String platform = CloudPlatform.AZURE.name();
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                Platform.platform(platform.toUpperCase(Locale.ROOT)),
                Variant.variant(platform.toUpperCase(Locale.ROOT)));

        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(connector);
        when(connector.parameters()).thenReturn(platformParameters);
        when(platformParameters.imageFilter()).thenReturn(Optional.of(imageFilter));
        when(imageToCoreImageConverter.convert(image)).thenReturn(coreImage);
        when(imageToCoreImageConverter.convert(imageToBeFiltered)).thenReturn(coreImageToBeFiltered);
        when(imageFilter.filterImages(coreImageList)).thenReturn(List.of(coreImage));
        when(coreImageToImageConverter.convert(coreImage)).thenReturn(image);

        List<Image> filteredImages = underTest.filterImages(platform, imageList);

        assertEquals(1, filteredImages.size());
        assertEquals(image, filteredImages.get(0));
        verify(cloudPlatformConnectors).get(cloudPlatformVariant);
    }

    @Test
    public void testForAnInvalidProvider() {
        Image image = mock(Image.class);
        Image imageToBeFiltered = mock(Image.class);
        List<Image> imageList = List.of(image, imageToBeFiltered);
        String platform = CloudPlatform.AZURE.name() + "xyz";
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                Platform.platform(platform.toUpperCase(Locale.ROOT)),
                Variant.variant(platform.toUpperCase(Locale.ROOT)));

        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(null);

        List<Image> filteredImages = underTest.filterImages(platform, imageList);

        assertEquals(2, filteredImages.size());
        verify(cloudPlatformConnectors).get(cloudPlatformVariant);
    }
}