package com.sequenceiq.cloudbreak.cloud.azure.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.azure.AzureImage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorageAccountService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;

@ExtendWith(MockitoExtension.class)
public class AzureImageSetupServiceTest {

    private static final String REGION = "aRegion";

    @InjectMocks
    private AzureImageSetupService underTest;

    @Mock
    private AzureImageFormatValidator azureImageFormatValidator;

    @Mock
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Mock
    private AzureStorage armStorage;

    @Mock
    private AzureImageInfoService azureImageInfoService;

    @Mock
    private AzureImageService azureImageService;

    @Mock
    private AzureStorageAccountService azureStorageAccountService;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private CloudStack stack;

    @Mock
    private Image image;

    @Mock
    private AzureClient client;

    @BeforeEach
    public void setUp() {
        lenient().when(azureResourceGroupMetadataProvider.getResourceGroupName(any(), any(CloudStack.class))).thenReturn("resourceGroupName");
        lenient().when(azureResourceGroupMetadataProvider.getImageResourceGroupName(any(), any())).thenReturn("imageResourceGroupName");
        lenient().when(azureImageInfoService.getImageInfo(any(), any(), any(), any())).thenReturn(getAzureImageInfo());
        lenient().when(ac.getCloudCredential()).thenReturn(mock(CloudCredential.class));
    }

    @Test
    void testCopyVhdImageIfNecessaryWhenImageIsMarketplace() {
        when(azureImageFormatValidator.isMarketplaceImageFormat(image)).thenReturn(true);

        underTest.copyVhdImageIfNecessary(ac, stack, image, REGION, client);

        verifyNoInteractions(azureResourceGroupMetadataProvider);
        verifyNoInteractions(armStorage);
        verifyNoInteractions(azureImageInfoService);
        verifyNoInteractions(azureImageService);
        verifyNoInteractions(azureStorageAccountService);
        verifyNoInteractions(client);
    }

    @Test
    void testCopyVhdImageIfNecessaryWhenImageNotInStorage() {
        when(azureImageFormatValidator.isMarketplaceImageFormat(image)).thenReturn(false);
        when(armStorage.getImageStorageName(any(), any(), any())).thenReturn("imageStorageName");
        when(azureImageService.findImage(any(), any(), any())).thenReturn(Optional.empty());

        underTest.copyVhdImageIfNecessary(ac, stack, image, REGION, client);

        verify(azureResourceGroupMetadataProvider).getResourceGroupName(any(), any(CloudStack.class));
        verify(armStorage).getImageStorageName(any(), any(), any());
        verify(azureResourceGroupMetadataProvider).getImageResourceGroupName(any(), any());
        verify(azureImageInfoService).getImageInfo(any(), any(), any(), any());
        verify(azureImageService).findImage(any(), any(), any());
        verify(azureStorageAccountService).createStorageAccount(any(), any(), any(), any(), any(), any());
        verify(azureStorageAccountService).createContainerInStorage(any(), any(), any());
        verify(client).copyImageBlobInStorageContainer(any(), any(), any(), any(), any());
    }

    @Test
    void testCopyVhdImageIfNecessaryWhenImageExistsInStorage() {
        when(azureImageFormatValidator.isMarketplaceImageFormat(image)).thenReturn(false);
        when(azureImageService.findImage(any(), any(), any())).thenReturn(Optional.of(new AzureImage("id", "name", true)));

        underTest.copyVhdImageIfNecessary(ac, stack, image, REGION, client);

        verify(armStorage).getImageStorageName(any(), any(), any());
        verifyNoInteractions(azureStorageAccountService);
        verifyNoInteractions(client);
    }

    @Test
    void testCopyVhdImageIfNecessaryWhenException() {
        when(azureImageFormatValidator.isMarketplaceImageFormat(image)).thenReturn(false);
        when(armStorage.getImageStorageName(any(), any(), any())).thenReturn("imageStorageName");
        when(azureImageService.findImage(any(), any(), any())).thenReturn(Optional.empty());

        doThrow(new CloudConnectorException("Test exception")).when(client).copyImageBlobInStorageContainer(any(), any(), any(), any(), any());

        CloudConnectorException exception = assertThrows(CloudConnectorException.class, () ->
                underTest.copyVhdImageIfNecessary(ac, stack, image, REGION, client));
        assertEquals("Test exception", exception.getMessage());
    }

    private AzureImageInfo getAzureImageInfo() {
        return new AzureImageInfo("", "", "", "", "");
    }
}