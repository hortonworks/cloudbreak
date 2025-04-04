package com.sequenceiq.cloudbreak.cloud.azure.image;

import static com.sequenceiq.cloudbreak.cloud.azure.AzureStorage.IMAGES_CONTAINER;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.azure.resourcemanager.compute.models.VirtualMachineCustomImage;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.CopyStatusType;
import com.sequenceiq.cloudbreak.cloud.azure.AzureImage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorageAccountService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.CopyState;
import com.sequenceiq.cloudbreak.cloud.azure.util.CustomVMImageNameProvider;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudImageFallbackException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.PrepareImageType;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.type.ImageStatus;
import com.sequenceiq.common.api.type.ImageStatusResult;

@Service
public class AzureImageSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureImageSetupService.class);

    @Inject
    private AzureStorage armStorage;

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Inject
    private AzureStorageAccountService azureStorageAccountService;

    @Inject
    private AzureImageService azureImageService;

    @Inject
    private AzureManagedImageService azureManagedImageService;

    @Inject
    private AzureImageInfoService azureImageInfoService;

    @Inject
    private CustomVMImageNameProvider customVMImageNameProvider;

    @Inject
    private AzureImageFormatValidator azureImageFormatValidator;

    @Inject
    private AzureMarketplaceValidatorService azureMarketplaceValidatorService;

    public ImageStatusResult checkImageStatus(AuthenticatedContext ac, CloudStack stack, Image image) {

        if (azureImageFormatValidator.isMarketplaceImageFormat(image)) {
            LOGGER.info("Skipping image copy check as target image ({}) is an Azure Marketplace image!", image.getImageName());
            return new ImageStatusResult(ImageStatus.CREATE_FINISHED, ImageStatusResult.COMPLETED);
        }
        CloudContext cloudContext = ac.getCloudContext();
        String imageResourceGroupName = azureResourceGroupMetadataProvider.getImageResourceGroupName(cloudContext, stack);
        AzureClient client = ac.getParameter(AzureClient.class);

        AzureImageInfo azureImageInfo = azureImageInfoService.getImageInfo(imageResourceGroupName, image.getImageName(), ac, client);
        Optional<VirtualMachineCustomImage> customImage = azureManagedImageService.findVirtualMachineCustomImage(azureImageInfo, client);
        if (customImage.isPresent()) {
            LOGGER.info("Custom image with id {} already exists in the target resource group {}, bypassing VHD copy check!", customImage.get().id(),
                    imageResourceGroupName);
            return new ImageStatusResult(ImageStatus.CREATE_FINISHED, ImageStatusResult.COMPLETED);
        }
        AzureCredentialView acv = new AzureCredentialView(ac.getCloudCredential());
        String imageStorageName = armStorage.getImageStorageName(acv, cloudContext, stack);
        try {
            return getImageStatusResult(imageResourceGroupName, client, azureImageInfo, imageStorageName);
        } catch (RuntimeException ex) {
            String msg = String.format("Failed to check the status of the image in resource group '%s', image storage name '%s'",
                    imageResourceGroupName, imageStorageName);
            LOGGER.error(msg, ex);
            return new ImageStatusResult(ImageStatus.CREATE_FAILED, ImageStatusResult.INIT);
        }
    }

    private ImageStatusResult getImageStatusResult(String imageResourceGroupName, AzureClient client, AzureImageInfo azureImageInfo, String imageStorageName) {
        CopyState copyState = client.getCopyStatus(imageResourceGroupName, imageStorageName, IMAGES_CONTAINER, azureImageInfo.getImageName());
        boolean storageContainsImage = storageContainsImage(client, imageResourceGroupName, imageStorageName, azureImageInfo.getImageName());
        if ((copyState.getCopyStatusType() == null || CopyStatusType.SUCCESS.equals(copyState.getCopyStatusType())) && storageContainsImage) {
            LOGGER.debug("The copy has been finished because the storage account already contains the image.");
            return new ImageStatusResult(ImageStatus.CREATE_FINISHED, ImageStatusResult.COMPLETED);
        }
        if (CopyStatusType.SUCCESS.equals(copyState.getCopyStatusType())) {
            LOGGER.error("The image has not been found in the storage account.");
            return new ImageStatusResult(ImageStatus.CREATE_FAILED, ImageStatusResult.COMPLETED);
        } else if (isCopyStatusFailed(copyState.getCopyStatusType())) {
            LOGGER.error("The image copy has failed with status: {}", copyState);
            return new ImageStatusResult(ImageStatus.CREATE_FAILED, 0);
        } else {
            int percentage = (int) (((double) copyState.getBytesCopied() * ImageStatusResult.COMPLETED) / copyState.getTotalBytes());
            LOGGER.info("CopyStatus, Total:{} / Pending:{} bytes, {}%", copyState.getTotalBytes(), copyState.getBytesCopied(), percentage);
            return new ImageStatusResult(ImageStatus.IN_PROGRESS, percentage);
        }
    }

    private boolean isCopyStatusFailed(CopyStatusType copyState) {
        return CopyStatusType.ABORTED.equals(copyState) || CopyStatusType.FAILED.equals(copyState);
    }

    public void validateImage(AuthenticatedContext ac, CloudStack stack, Image image, AzureClient client) {
        azureMarketplaceValidatorService.validateMarketplaceImageTermsForOsUpgrade(image, client, stack, ac);
    }

    public void copyVhdImageIfNecessary(AuthenticatedContext ac, CloudStack stack, Image image, String region, AzureClient client,
            PrepareImageType prepareType, String imageFallbackTarget) {
        CloudContext cloudContext = ac.getCloudContext();
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        MarketplaceValidationResult validationResult = azureMarketplaceValidatorService.validateMarketplaceImage(
                image, prepareType, imageFallbackTarget, client, stack, ac);
        boolean fallbackRequired = validationResult.isFallbackRequired();
        boolean skipVhdCopy = validationResult.isSkipVhdCopy();
        boolean hasFallbackImage = StringUtils.isNotBlank(imageFallbackTarget);
        if (fallbackRequired) {
            if (hasFallbackImage) {
                image.setImageName(imageFallbackTarget);
            } else {
                LOGGER.warn("Fallback would be required but there is no fallback image available for image {}", image.getImageName());
            }
        }
        if (!skipVhdCopy) {
            String imageStorageName = armStorage.getImageStorageName(new AzureCredentialView(ac.getCloudCredential()), cloudContext, stack);
            String imageResourceGroupName = azureResourceGroupMetadataProvider.getImageResourceGroupName(cloudContext, stack);

            AzureImageInfo azureImageInfo = azureImageInfoService.getImageInfo(imageResourceGroupName, image.getImageName(), ac, client);
            Optional<AzureImage> foundImage = azureImageService.findImage(azureImageInfo, client, ac);
            if (foundImage.isPresent()) {
                LOGGER.info("Custom image with id {} already exists in the target resource group {}, bypassing VHD check!",
                        foundImage.get().getId(), imageResourceGroupName);
            } else {
                LOGGER.info("Custom image with name {} does not exist in the target resource group {}, checking VHD!",
                        azureImageInfo.getImageName(), imageResourceGroupName);

                createRequiredCloudObjects(ac, stack, region, client, resourceGroupName, imageResourceGroupName, imageStorageName);
                if (!storageContainsImage(client, imageResourceGroupName, imageStorageName, azureImageInfo.getImageName())) {
                    performCopy(image, client, imageStorageName, imageResourceGroupName, azureImageInfo, validationResult);
                } else {
                    LOGGER.info("The image already exists in the storage account.");
                }
                if (fallbackRequired) {
                    if (hasFallbackImage) {
                        throw new CloudImageFallbackException("Fallback required");
                    } else {
                        LOGGER.warn("Fallback would be required but there is no fallback image available for image {}", image.getImageName());
                    }
                }
            }
        }
    }

    private void createRequiredCloudObjects(AuthenticatedContext ac, CloudStack stack, String region, AzureClient client, String resourceGroupName,
            String imageResourceGroupName, String imageStorageName) {
        createResourceGroupIfNotExists(client, resourceGroupName, region, stack);
        createResourceGroupIfNotExists(client, imageResourceGroupName, region, stack);
        azureStorageAccountService.createStorageAccount(ac, client, imageResourceGroupName, imageStorageName, region, stack);
        azureStorageAccountService.createContainerInStorage(client, imageResourceGroupName, imageStorageName);
    }

    private void performCopy(Image image, AzureClient client, String imageStorageName, String imageResourceGroupName, AzureImageInfo azureImageInfo,
            MarketplaceValidationResult validationResult) {
        try {
            LOGGER.info("Starting to copy image: {}, into storage account: {}", image.getImageName(), imageStorageName);
            client.copyImageBlobInStorageContainer(
                    imageResourceGroupName, imageStorageName, IMAGES_CONTAINER, image.getImageName(), azureImageInfo.getImageName());
        } catch (CloudConnectorException e) {
            LOGGER.warn("Something happened during start image copy.", e);
            ValidationResult whatIfResult = validationResult.getValidationResult();
            if (whatIfResult != null && whatIfResult.hasErrorOrWarning()) {
                String errorMessage = String.format(" VHD is copied over as a fallback mechanism as you seem to have Azure Marketplace image " +
                                "but its terms are not yet accepted and CDP_AZURE_IMAGE_MARKETPLACE_ONLY is not granted " +
                                "so we tried to pre-validate the deployment, " +
                                "but it failed with the following error, please correct it and try again: %s",
                        whatIfResult.getFormattedErrors());
                throw new CloudConnectorException(e.getMessage() + errorMessage);
            }
            throw e;
        }
    }

    public Optional<AzureImageCopyDetails> getImageCopyDetails(AuthenticatedContext ac, CloudStack stack, Image image) {
        try {
            if (azureImageFormatValidator.isMarketplaceImageFormat(image)) {
                LOGGER.info("Skipping gathering image copy details as target image ({}) is an Azure Marketplace image!", image.getImageName());
                return Optional.empty();
            }

            CloudContext cloudContext = ac.getCloudContext();
            String imageStorageName = armStorage.getImageStorageName(new AzureCredentialView(ac.getCloudCredential()), cloudContext, stack);
            String imageResourceGroupName = azureResourceGroupMetadataProvider.getImageResourceGroupName(cloudContext, stack);
            return Optional.of(new AzureImageCopyDetails(imageStorageName, imageResourceGroupName, image.getImageName()));
        } catch (Exception e) {
            LOGGER.warn("Could not gather image copy details ", e);
            return Optional.empty();
        }
    }

    private boolean storageContainsImage(AzureClient client, String resourceGroupName, String storageName, String imageName) {
        List<BlobItem> blobItems = client.listBlobInStorage(resourceGroupName, storageName, IMAGES_CONTAINER);
        for (BlobItem blobItem : blobItems) {
            if (customVMImageNameProvider.getImageNameFromConnectionString(blobItem.getName()).equals(imageName)) {
                LOGGER.info("The storage account {} in {} resource group contains the requested image {}", storageName, resourceGroupName, imageName);
                return true;
            }
        }
        LOGGER.info("The storage account {} in {} resource group does not contains the requested image {}", storageName, resourceGroupName, imageName);
        return false;
    }

    private void createResourceGroupIfNotExists(AzureClient client, String resourceGroupName, String region, CloudStack stack) {
        if (!client.resourceGroupExists(resourceGroupName)) {
            LOGGER.info("Creating resource group: {}", resourceGroupName);
            client.createResourceGroup(resourceGroupName, region, stack.getTags());
        }
    }

}