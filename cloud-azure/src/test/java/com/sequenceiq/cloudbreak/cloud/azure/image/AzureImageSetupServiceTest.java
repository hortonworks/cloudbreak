package com.sequenceiq.cloudbreak.cloud.azure.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.storage.blob.models.BlobItem;
import com.sequenceiq.cloudbreak.cloud.azure.AzureImage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorageAccountService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.util.CustomVMImageNameProvider;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudImageFallbackException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.PrepareImageType;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

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

    @Mock
    private AzureMarketplaceValidatorService azureMarketplaceValidatorService;

    @Mock
    private CustomVMImageNameProvider customVMImageNameProvider;

    @BeforeEach
    public void setUp() {
        lenient().when(azureResourceGroupMetadataProvider.getResourceGroupName(any(), any(CloudStack.class))).thenReturn("resourceGroupName");
        lenient().when(azureResourceGroupMetadataProvider.getImageResourceGroupName(any(), any())).thenReturn("imageResourceGroupName");
        lenient().when(azureImageInfoService.getImageInfo(any(), any(), any(), any())).thenReturn(getAzureImageInfo());
        lenient().when(ac.getCloudCredential()).thenReturn(mock(CloudCredential.class));
        lenient().when(azureMarketplaceValidatorService.validateMarketplaceImage(any(), any(), any(), any(), any(), any())).
                thenReturn(new MarketplaceValidationResult(false, false));
    }

    @ParameterizedTest
    @MethodSource("parameterScenarios")
    void testCopyVhdImageIfNecessaryWhenImageIsMarketplaceWithFallback(boolean skipCopy, boolean hasFallback) {
        when(azureMarketplaceValidatorService.validateMarketplaceImage(any(), any(), any(), any(), any(), any())).
                thenReturn(new MarketplaceValidationResult(true, skipCopy));

        try {
            underTest.copyVhdImageIfNecessary(ac, stack, image, REGION, client, PrepareImageType.EXECUTED_DURING_PROVISIONING,
                    hasFallback ? "fallback" : "");

            if (hasFallback) {
                verify(image).setImageName(any());
            } else {
                verify(image, never()).setImageName(any());
            }
            if (skipCopy) {
                verify(azureResourceGroupMetadataProvider).getResourceGroupName(any(), any(CloudStack.class));
                verifyNoInteractions(armStorage);
                verifyNoInteractions(azureImageInfoService);
                verifyNoInteractions(azureImageService);
                verifyNoInteractions(azureStorageAccountService);
                verifyNoInteractions(client);

            } else {
                verify(azureResourceGroupMetadataProvider).getResourceGroupName(any(), any(CloudStack.class));
                verify(armStorage).getImageStorageName(any(), any(), any());
                verify(azureResourceGroupMetadataProvider).getImageResourceGroupName(any(), any());
                verify(azureImageInfoService).getImageInfo(any(), any(), any(), any());
                verify(azureImageService).findImage(any(), any(), any());
                verify(azureStorageAccountService).createStorageAccount(any(), any(), any(), any(), any(), any());
                verify(azureStorageAccountService).createContainerInStorage(any(), any(), any());
                verify(client).copyImageBlobInStorageContainer(any(), any(), any(), hasFallback ? eq("fallback") : any(), any());
            }
        } catch (CloudImageFallbackException e) {
            assertEquals("Fallback required", e.getMessage());
            assertFalse(skipCopy);
            assertTrue(hasFallback);
        }
    }

    private static Stream<Arguments> parameterScenarios() {
        return Stream.of(
                Arguments.of(true, true),
                Arguments.of(true, false),
                Arguments.of(false, true),
                Arguments.of(false, false));
    }

    @Test
    void testCopyVhdImageIfNecessaryWhenImageNotInStorage() {
        when(armStorage.getImageStorageName(any(), any(), any())).thenReturn("imageStorageName");
        when(azureImageService.findImage(any(), any(), any())).thenReturn(Optional.empty());

        underTest.copyVhdImageIfNecessary(ac, stack, image, REGION, client, PrepareImageType.EXECUTED_DURING_PROVISIONING, null);

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
    void testCopyVhdImageIfNecessaryWhenImageBlobAlreadyPresentInStorageAccount() {
        when(armStorage.getImageStorageName(any(), any(), any())).thenReturn("imageStorageName");
        when(image.getImageName()).thenReturn("anImageName");
        when(azureImageService.findImage(any(), any(), any())).thenReturn(Optional.empty());
        when(client.listBlobInStorage(any(), any(), any())).thenReturn(List.of(new BlobItem()));
        when(customVMImageNameProvider.getImageNameFromConnectionString(any())).thenReturn("anImageName");

        underTest.copyVhdImageIfNecessary(ac, stack, image, REGION, client, PrepareImageType.EXECUTED_DURING_PROVISIONING, null);

        verify(azureResourceGroupMetadataProvider).getResourceGroupName(any(), any(CloudStack.class));
        verify(armStorage).getImageStorageName(any(), any(), any());
        verify(azureResourceGroupMetadataProvider).getImageResourceGroupName(any(), any());
        verify(azureImageInfoService).getImageInfo(any(), any(), any(), any());
        verify(azureImageService).findImage(any(), any(), any());
        verify(azureStorageAccountService).createStorageAccount(any(), any(), any(), any(), any(), any());
        verify(azureStorageAccountService).createContainerInStorage(any(), any(), any());
        verify(client, never()).copyImageBlobInStorageContainer(any(), any(), any(), any(), any());
    }

    @Test
    void testCopyVhdImageIfNecessaryWhenImageExistsInStorage() {
        when(azureImageService.findImage(any(), any(), any())).thenReturn(Optional.of(new AzureImage("id", "name", true)));

        underTest.copyVhdImageIfNecessary(ac, stack, image, REGION, client, PrepareImageType.EXECUTED_DURING_PROVISIONING, null);

        verify(armStorage).getImageStorageName(any(), any(), any());
        verifyNoInteractions(azureStorageAccountService);
        verifyNoInteractions(client);
    }

    @Test
    void testCopyVhdImageIfNecessaryWhenException() {
        when(armStorage.getImageStorageName(any(), any(), any())).thenReturn("imageStorageName");
        when(azureImageService.findImage(any(), any(), any())).thenReturn(Optional.empty());

        doThrow(new CloudConnectorException("Test exception")).when(client).copyImageBlobInStorageContainer(any(), any(), any(), any(), any());

        CloudConnectorException exception = assertThrows(CloudConnectorException.class, () ->
                underTest.copyVhdImageIfNecessary(ac, stack, image, REGION, client, PrepareImageType.EXECUTED_DURING_PROVISIONING, null));
        assertEquals("Test exception", exception.getMessage());
    }

    @Test
    void testCopyVhdImageIfNecessaryWhenExceptionAndValidationResultError() {
        when(armStorage.getImageStorageName(any(), any(), any())).thenReturn("imageStorageName");
        when(azureImageService.findImage(any(), any(), any())).thenReturn(Optional.empty());
        MarketplaceValidationResult validationResult = new MarketplaceValidationResult(true,
                ValidationResult.builder().error("Validation Error").build(), false);
        when(azureMarketplaceValidatorService.validateMarketplaceImage(image, PrepareImageType.EXECUTED_DURING_PROVISIONING, "imageFallbackTarget",
                client, stack, ac)).thenReturn(validationResult);

        doThrow(new CloudConnectorException("Test exception")).when(client).copyImageBlobInStorageContainer(any(), any(), any(), any(), any());

        CloudConnectorException exception = assertThrows(CloudConnectorException.class, () ->
                underTest.copyVhdImageIfNecessary(ac, stack, image, REGION, client, PrepareImageType.EXECUTED_DURING_PROVISIONING, "imageFallbackTarget"));
        assertEquals("Test exception VHD is copied over as a fallback mechanism as you seem to have Azure Marketplace image but its terms are not yet accepted" +
                " and CDP_AZURE_IMAGE_MARKETPLACE_ONLY is not granted so we tried to pre-validate the deployment, but it failed with the following error, " +
                "please correct it and try again: Validation Error", exception.getMessage());
    }

    @Test
    void testCopyVhdImageIfNecessaryWhenExceptionAndValidationResultWarning() {
        MarketplaceValidationResult validationResult = new MarketplaceValidationResult(true,
                ValidationResult.builder().warning("Validation warning").build(), true);
        when(azureMarketplaceValidatorService.validateMarketplaceImage(image, PrepareImageType.EXECUTED_DURING_PROVISIONING, "imageFallbackTarget",
                client, stack, ac)).thenReturn(validationResult);

        underTest.copyVhdImageIfNecessary(ac, stack, image, REGION, client, PrepareImageType.EXECUTED_DURING_PROVISIONING, "imageFallbackTarget");
        verify(client, never()).copyImageBlobInStorageContainer(any(), any(), any(), any(), any());
    }

    private AzureImageInfo getAzureImageInfo() {
        return new AzureImageInfo("", "anImageName", "", "", "");
    }
}