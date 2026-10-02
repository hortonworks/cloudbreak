package com.sequenceiq.cloudbreak.cloud.azure.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.compute.fluent.models.ImageInner;
import com.azure.resourcemanager.compute.models.HyperVGenerationTypes;
import com.azure.resourcemanager.compute.models.VirtualMachineCustomImage;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

@ExtendWith(MockitoExtension.class)
public class AzureManagedImageServiceTest {

    private static final String RESOURCE_GROUP = "resource-group";

    private static final String IMAGE_NAME_WITH_REGION = "image-name-with-region";

    private static final String IMAGE_NAME = "image-name";

    @InjectMocks
    private AzureManagedImageService underTest;

    @Mock
    private AzureClient azureClient;

    @Spy
    private final AzureExceptionHandler azureExceptionHandler = new AzureExceptionHandler();

    @Test
    public void testGetVirtualMachineCustomImageShouldReturnTheImageWhenSucceededOnProviderSide() {
        AzureImageInfo azureImageInfo = imageInfoWithGeneration(null);
        VirtualMachineCustomImage image = mock(VirtualMachineCustomImage.class);
        when(azureClient.findImage(RESOURCE_GROUP, IMAGE_NAME_WITH_REGION)).thenReturn(Optional.ofNullable(image));
        ImageInner imageInner = mock(ImageInner.class);
        when(image.innerModel()).thenReturn(imageInner);
        when(imageInner.provisioningState()).thenReturn("Succeeded");

        Optional<VirtualMachineCustomImage> actual = underTest.findVirtualMachineCustomImage(azureImageInfo, azureClient);

        assertTrue(actual.isPresent());
        assertEquals(image, actual.get());
        verify(azureClient).findImage(RESOURCE_GROUP, IMAGE_NAME_WITH_REGION);
    }

    @Test
    public void testGetVirtualMachineCustomImageShouldReturnTheImageWhenUnknownOnProviderSide() {
        AzureImageInfo azureImageInfo = imageInfoWithGeneration(null);
        VirtualMachineCustomImage image = mock(VirtualMachineCustomImage.class);
        when(azureClient.findImage(RESOURCE_GROUP, IMAGE_NAME_WITH_REGION)).thenReturn(Optional.ofNullable(image));
        ImageInner imageInner = mock(ImageInner.class);
        when(image.innerModel()).thenReturn(imageInner);
        when(imageInner.provisioningState()).thenReturn("Unknown");

        Optional<VirtualMachineCustomImage> actual = underTest.findVirtualMachineCustomImage(azureImageInfo, azureClient);

        assertTrue(actual.isEmpty());
        verify(azureClient).findImage(RESOURCE_GROUP, IMAGE_NAME_WITH_REGION);
    }

    @Test
    public void testGetVirtualMachineCustomImageShouldReturnTheImageWhenFailedOnProviderSide() {
        AzureImageInfo azureImageInfo = imageInfoWithGeneration(null);
        VirtualMachineCustomImage image = mock(VirtualMachineCustomImage.class);
        when(azureClient.findImage(RESOURCE_GROUP, IMAGE_NAME_WITH_REGION)).thenReturn(Optional.ofNullable(image));
        ImageInner imageInner = mock(ImageInner.class);
        when(image.innerModel()).thenReturn(imageInner);
        when(imageInner.provisioningState()).thenReturn("Failed");

        CloudConnectorException actual = assertThrows(CloudConnectorException.class, () -> underTest.findVirtualMachineCustomImage(azureImageInfo, azureClient));

        assertEquals("Image (resource-group/image-name-with-region) creation failed in Azure, please check the reason on Azure Portal!", actual.getMessage());
        verify(azureClient).findImage(RESOURCE_GROUP, IMAGE_NAME_WITH_REGION);
    }

    @Test
    public void testGetVirtualMachineCustomImageShouldReturnTheImageWhenDoesNotExistOnProviderSide() {
        AzureImageInfo azureImageInfo = imageInfoWithGeneration(null);
        when(azureClient.findImage(RESOURCE_GROUP, IMAGE_NAME_WITH_REGION)).thenReturn(Optional.empty());

        Optional<VirtualMachineCustomImage> actual = underTest.findVirtualMachineCustomImage(azureImageInfo, azureClient);

        assertTrue(actual.isEmpty());
        verify(azureClient).findImage(RESOURCE_GROUP, IMAGE_NAME_WITH_REGION);
    }

    @ParameterizedTest
    @ValueSource(strings = {"V1", "V2"})
    public void testGetVirtualMachineCustomImageShouldReturnTheImageWhenHyperVGenerationMatches(String generation) {
        AzureImageInfo azureImageInfo = imageInfoWithGeneration(HyperVGenerationTypes.fromString(generation));
        VirtualMachineCustomImage image = succeededImage();
        when(image.hyperVGeneration()).thenReturn(HyperVGenerationTypes.fromString(generation));

        Optional<VirtualMachineCustomImage> actual = underTest.findVirtualMachineCustomImage(azureImageInfo, azureClient);

        assertTrue(actual.isPresent());
        assertEquals(image, actual.get());
    }

    @ParameterizedTest
    @CsvSource({"V1, V2", "V2, V1"})
    public void testGetVirtualMachineCustomImageShouldThrowWithoutCheckingStateWhenHyperVGenerationMismatches(String requiredGeneration,
            String existingGeneration) {
        AzureImageInfo azureImageInfo = imageInfoWithGeneration(HyperVGenerationTypes.fromString(requiredGeneration));
        VirtualMachineCustomImage image = mock(VirtualMachineCustomImage.class);
        when(azureClient.findImage(RESOURCE_GROUP, IMAGE_NAME_WITH_REGION)).thenReturn(Optional.of(image));
        ImageInner imageInner = mock(ImageInner.class);
        when(image.innerModel()).thenReturn(imageInner);
        when(image.hyperVGeneration()).thenReturn(HyperVGenerationTypes.fromString(existingGeneration));

        CloudConnectorException actual = assertThrows(CloudConnectorException.class, () -> underTest.findVirtualMachineCustomImage(azureImageInfo, azureClient));

        assertEquals(String.format("Image (resource-group/image-name-with-region) is Hyper-V generation %s, but %s is required. Azure does not allow "
                + "changing the generation of an existing image, please delete or rename it on Azure Portal!", existingGeneration, requiredGeneration),
                actual.getMessage());
        verify(imageInner, never()).provisioningState();
    }

    @Test
    public void testGetVirtualMachineCustomImageShouldReturnTheImageWhenExistingImageHasNoHyperVGeneration() {
        AzureImageInfo azureImageInfo = imageInfoWithGeneration(HyperVGenerationTypes.V2);
        VirtualMachineCustomImage image = succeededImage();
        when(image.hyperVGeneration()).thenReturn(null);

        Optional<VirtualMachineCustomImage> actual = underTest.findVirtualMachineCustomImage(azureImageInfo, azureClient);

        assertTrue(actual.isPresent());
        assertEquals(image, actual.get());
    }

    @Test
    public void testGetVirtualMachineCustomImageShouldReturnTheImageWithoutCheckingGenerationWhenNoGenerationIsRequired() {
        AzureImageInfo azureImageInfo = imageInfoWithGeneration(null);
        VirtualMachineCustomImage image = succeededImage();

        Optional<VirtualMachineCustomImage> actual = underTest.findVirtualMachineCustomImage(azureImageInfo, azureClient);

        assertTrue(actual.isPresent());
        assertEquals(image, actual.get());
        verify(image, never()).hyperVGeneration();
    }

    private AzureImageInfo imageInfoWithGeneration(HyperVGenerationTypes hyperVGeneration) {
        return new AzureImageInfo(IMAGE_NAME_WITH_REGION, IMAGE_NAME, "imageId", "region", RESOURCE_GROUP, hyperVGeneration);
    }

    private VirtualMachineCustomImage succeededImage() {
        VirtualMachineCustomImage image = mock(VirtualMachineCustomImage.class);
        when(azureClient.findImage(RESOURCE_GROUP, IMAGE_NAME_WITH_REGION)).thenReturn(Optional.of(image));
        ImageInner imageInner = mock(ImageInner.class);
        when(image.innerModel()).thenReturn(imageInner);
        when(imageInner.provisioningState()).thenReturn("Succeeded");
        return image;
    }
}