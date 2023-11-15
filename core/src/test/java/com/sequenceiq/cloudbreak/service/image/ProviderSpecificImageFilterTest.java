package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
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
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@ExtendWith(MockitoExtension.class)
class ProviderSpecificImageFilterTest {

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @InjectMocks
    private ProviderSpecificImageFilter underTest;

    @Test
    public void testForAValidProvider() {
        CloudConnector connector = mock(CloudConnector.class);
        PlatformParameters platformParameters = mock(PlatformParameters.class);
        AzureImageFilter imageFilter = mock(AzureImageFilter.class);
        Image image = mockImageWithProvider(CloudPlatform.AZURE);
        Image imageToBeFiltered = mockImageWithProvider(CloudPlatform.AZURE);
        List<Image> imageList = List.of(image, imageToBeFiltered);
        ImageCatalogPlatform platform = imageCatalogPlatform(CloudPlatform.AZURE.name());
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                Platform.platform(platform.nameToUpperCase()),
                Variant.variant(platform.nameToUpperCase()));

        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(connector);
        when(connector.parameters()).thenReturn(platformParameters);
        when(platformParameters.imageFilter()).thenReturn(Optional.of(imageFilter));
        when(imageFilter.filterImages(imageList)).thenReturn(List.of(image));

        List<Image> filteredImages = underTest.filterImages(Collections.singletonList(platform), imageList);

        assertEquals(1, filteredImages.size());
        assertEquals(image, filteredImages.get(0));
        verify(cloudPlatformConnectors).get(cloudPlatformVariant);
    }

    @Test
    public void testProviderPreFiltering() {
        CloudConnector connector = mock(CloudConnector.class);
        PlatformParameters platformParameters = mock(PlatformParameters.class);
        AzureImageFilter imageFilter = mock(AzureImageFilter.class);
        Image image = mockImageWithProvider(CloudPlatform.AZURE);
        Image imageToBeFiltered = mockImageWithProvider(CloudPlatform.AWS);
        List<Image> imageList = List.of(image, imageToBeFiltered);
        ImageCatalogPlatform platform = imageCatalogPlatform(CloudPlatform.AZURE.name());
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                Platform.platform(platform.nameToUpperCase()),
                Variant.variant(platform.nameToUpperCase()));

        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(connector);
        when(connector.parameters()).thenReturn(platformParameters);
        when(platformParameters.imageFilter()).thenReturn(Optional.of(imageFilter));
        when(imageFilter.filterImages(List.of(image))).thenReturn(List.of(image));

        List<Image> filteredImages = underTest.filterImages(Collections.singletonList(platform), imageList);

        assertEquals(1, filteredImages.size());
        assertEquals(image, filteredImages.get(0));
        verify(cloudPlatformConnectors).get(cloudPlatformVariant);
    }

    @Test
    public void testForAnInvalidProvider() {
        Image image = mock(Image.class);
        Image imageToBeFiltered = mock(Image.class);
        List<Image> imageList = List.of(image, imageToBeFiltered);
        ImageCatalogPlatform platform = imageCatalogPlatform(CloudPlatform.AZURE.name() + "xyz");
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                Platform.platform(platform.nameToUpperCase()),
                Variant.variant(platform.nameToUpperCase()));

        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(null);

        List<Image> filteredImages = underTest.filterImages(Collections.singletonList(platform), imageList);

        assertEquals(0, filteredImages.size());
        verify(cloudPlatformConnectors).get(cloudPlatformVariant);
    }

    private Image mockImageWithProvider(CloudPlatform cloudPlatform) {
        Image mock = mock(Image.class);
        when(mock.getImageSetsByProvider()).thenReturn(Collections.singletonMap(cloudPlatform.name().toLowerCase(), null));

        return mock;
    }
}