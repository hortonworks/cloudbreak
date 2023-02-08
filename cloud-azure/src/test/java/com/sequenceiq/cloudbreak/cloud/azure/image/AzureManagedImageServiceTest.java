package com.sequenceiq.cloudbreak.cloud.azure.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.compute.fluent.models.ImageInner;
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
        AzureImageInfo azureImageInfo = new AzureImageInfo(IMAGE_NAME_WITH_REGION, IMAGE_NAME, "imageId", "region", RESOURCE_GROUP);
        VirtualMachineCustomImage image = mock(VirtualMachineCustomImage.class);
        when(azureClient.findImage(RESOURCE_GROUP, IMAGE_NAME_WITH_REGION)).thenReturn(image);
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
        AzureImageInfo azureImageInfo = new AzureImageInfo(IMAGE_NAME_WITH_REGION, IMAGE_NAME, "imageId", "region", RESOURCE_GROUP);
        VirtualMachineCustomImage image = mock(VirtualMachineCustomImage.class);
        when(azureClient.findImage(RESOURCE_GROUP, IMAGE_NAME_WITH_REGION)).thenReturn(image);
        ImageInner imageInner = mock(ImageInner.class);
        when(image.innerModel()).thenReturn(imageInner);
        when(imageInner.provisioningState()).thenReturn("Unknown");

        Optional<VirtualMachineCustomImage> actual = underTest.findVirtualMachineCustomImage(azureImageInfo, azureClient);

        assertTrue(actual.isEmpty());
        verify(azureClient).findImage(RESOURCE_GROUP, IMAGE_NAME_WITH_REGION);
    }

    @Test
    public void testGetVirtualMachineCustomImageShouldReturnTheImageWhenFailedOnProviderSide() {
        AzureImageInfo azureImageInfo = new AzureImageInfo(IMAGE_NAME_WITH_REGION, IMAGE_NAME, "imageId", "region", RESOURCE_GROUP);
        VirtualMachineCustomImage image = mock(VirtualMachineCustomImage.class);
        when(azureClient.findImage(RESOURCE_GROUP, IMAGE_NAME_WITH_REGION)).thenReturn(image);
        ImageInner imageInner = mock(ImageInner.class);
        when(image.innerModel()).thenReturn(imageInner);
        when(imageInner.provisioningState()).thenReturn("Failed");

        CloudConnectorException actual = assertThrows(CloudConnectorException.class, () -> underTest.findVirtualMachineCustomImage(azureImageInfo, azureClient));

        assertEquals("Image (resource-group/image-name-with-region) creation failed in Azure, please check the reason on Azure Portal!", actual.getMessage());
        verify(azureClient).findImage(RESOURCE_GROUP, IMAGE_NAME_WITH_REGION);
    }

    @Test
    public void testGetVirtualMachineCustomImageShouldReturnTheImageWhenDoesNotExistOnProviderSide() {
        AzureImageInfo azureImageInfo = new AzureImageInfo(IMAGE_NAME_WITH_REGION, IMAGE_NAME, "imageId", "region", RESOURCE_GROUP);
        when(azureClient.findImage(RESOURCE_GROUP, IMAGE_NAME_WITH_REGION)).thenReturn(null);

        Optional<VirtualMachineCustomImage> actual = underTest.findVirtualMachineCustomImage(azureImageInfo, azureClient);

        assertTrue(actual.isEmpty());
        verify(azureClient).findImage(RESOURCE_GROUP, IMAGE_NAME_WITH_REGION);
    }
}