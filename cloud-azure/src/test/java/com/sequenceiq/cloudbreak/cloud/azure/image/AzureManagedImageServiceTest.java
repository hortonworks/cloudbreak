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
import org.mockito.junit.MockitoJUnitRunner;

import com.microsoft.azure.management.compute.VirtualMachineCustomImage;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureAuthExceptionHandler;

@RunWith(MockitoJUnitRunner.class)
public class AzureManagedImageServiceTest {

    private static final String RESOURCE_GROUP = "resource-group";

    private static final String IMAGE_NAME = "image-name";

    @InjectMocks
    private AzureManagedImageService underTest;

    @Mock
    private AzureClient azureClient;

    @Spy
    private final AzureAuthExceptionHandler azureAuthExceptionHandler = new AzureAuthExceptionHandler();

    @Test
    public void testGetVirtualMachineCustomImageShouldReturnTheImageWhenExistsOnProviderSide() {
        VirtualMachineCustomImage image = Mockito.mock(VirtualMachineCustomImage.class);

        when(azureClient.findImage(RESOURCE_GROUP, IMAGE_NAME)).thenReturn(image);

        Optional<VirtualMachineCustomImage> actual = underTest.findVirtualMachineCustomImage(RESOURCE_GROUP, IMAGE_NAME, azureClient);

        assertTrue(actual.isPresent());
        assertEquals(image, actual.get());
        verify(azureClient).findImage(RESOURCE_GROUP, IMAGE_NAME);
    }

    @Test
    public void testGetVirtualMachineCustomImageShouldReturnTheImageWhenDoesNotExistsOnProviderSide() {
        when(azureClient.findImage(RESOURCE_GROUP, IMAGE_NAME)).thenReturn(null);

        Optional<VirtualMachineCustomImage> actual = underTest.findVirtualMachineCustomImage(RESOURCE_GROUP, IMAGE_NAME, azureClient);

        assertTrue(actual.isEmpty());
        verify(azureClient).findImage(RESOURCE_GROUP, IMAGE_NAME);
    }
}