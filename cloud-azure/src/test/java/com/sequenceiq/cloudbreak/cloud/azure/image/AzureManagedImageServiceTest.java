package com.sequenceiq.cloudbreak.cloud.azure.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachineCustomImage;
import com.microsoft.azure.management.compute.VirtualMachineCustomImages;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureAuthExceptionHandler;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Azure.class })
public class AzureManagedImageServiceTest {

    private static final String RESOURCE_GROUP = "resource-group";

    private static final String IMAGE_NAME = "image-name";

    @InjectMocks
    private AzureManagedImageService underTest;

    @Mock
    private AzureClient azureClient;

    @Mock
    private VirtualMachineCustomImages virtualMachineCustomImages;

    @Spy
    private final AzureAuthExceptionHandler azureAuthExceptionHandler = new AzureAuthExceptionHandler();

    @Test
    public void testGetVirtualMachineCustomImageShouldReturnTheImageWhenExistsOnProviderSide() {
        VirtualMachineCustomImage image = Mockito.mock(VirtualMachineCustomImage.class);
        Azure azure = PowerMockito.mock(Azure.class);

        when(azureClient.getAzure()).thenReturn(azure);
        when(azure.virtualMachineCustomImages()).thenReturn(virtualMachineCustomImages);
        when(virtualMachineCustomImages.getByResourceGroup(RESOURCE_GROUP, IMAGE_NAME)).thenReturn(image);

        Optional<VirtualMachineCustomImage> actual = underTest.findVirtualMachineCustomImage(RESOURCE_GROUP, IMAGE_NAME, azureClient);

        assertTrue(actual.isPresent());
        assertEquals(image, actual.get());
        verify(azureClient).getAzure();
        verify(azure).virtualMachineCustomImages();
        verify(virtualMachineCustomImages).getByResourceGroup(RESOURCE_GROUP, IMAGE_NAME);
    }

    @Test
    public void testGetVirtualMachineCustomImageShouldReturnTheImageWhenDoesNotExistsOnProviderSide() {
        Azure azure = PowerMockito.mock(Azure.class);

        when(azureClient.getAzure()).thenReturn(azure);
        when(azure.virtualMachineCustomImages()).thenReturn(virtualMachineCustomImages);
        when(virtualMachineCustomImages.getByResourceGroup(RESOURCE_GROUP, IMAGE_NAME)).thenReturn(null);

        Optional<VirtualMachineCustomImage> actual = underTest.findVirtualMachineCustomImage(RESOURCE_GROUP, IMAGE_NAME, azureClient);

        assertTrue(actual.isEmpty());
        verify(azureClient).getAzure();
        verify(azure).virtualMachineCustomImages();
        verify(virtualMachineCustomImages).getByResourceGroup(RESOURCE_GROUP, IMAGE_NAME);
    }
}