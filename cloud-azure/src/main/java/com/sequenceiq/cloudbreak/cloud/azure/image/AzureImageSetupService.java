package com.sequenceiq.cloudbreak.cloud.azure.image;

import static com.sequenceiq.cloudbreak.cloud.azure.AzureStorage.IMAGES_CONTAINER;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microsoft.azure.management.compute.VirtualMachineCustomImage;
import com.microsoft.azure.storage.blob.CopyState;
import com.microsoft.azure.storage.blob.CopyStatus;
import com.microsoft.azure.storage.blob.ListBlobItem;
import com.sequenceiq.cloudbreak.cloud.azure.AzureImage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorageAccountService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.util.CustomVMImageNameProvider;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
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
        if (copyState == null && storageContainsImage) {
            LOGGER.debug("The copy has been finished because the storage account already contains the image.");
            return new ImageStatusResult(ImageStatus.CREATE_FINISHED, ImageStatusResult.COMPLETED);
        } else if (copyState == null && !storageContainsImage) {
            throw new CloudConnectorException(
                    "Image copy failed because the copy state is not available and the storage account does not contains the image.");
        }
        if (CopyStatus.SUCCESS.equals(copyState.getStatus())) {
            if (!storageContainsImage) {
                LOGGER.error("The image has not been found in the storage account.");
                return new ImageStatusResult(ImageStatus.CREATE_FAILED, ImageStatusResult.COMPLETED);
            }
            LOGGER.info("The image copy has been finished.");
            return new ImageStatusResult(ImageStatus.CREATE_FINISHED, ImageStatusResult.COMPLETED);
        } else if (isCopyStatusFailed(copyState)) {
            LOGGER.error("The image copy has failed with status: {}", copyState.getStatus());
            return new ImageStatusResult(ImageStatus.CREATE_FAILED, 0);
        } else {
            int percentage = (int) (((double) copyState.getBytesCopied() * ImageStatusResult.COMPLETED) / copyState.getTotalBytes());
            LOGGER.info("CopyStatus, Total:{} / Pending:{} bytes, {}%", copyState.getTotalBytes(), copyState.getBytesCopied(), percentage);
            return new ImageStatusResult(ImageStatus.IN_PROGRESS, percentage);
        }
    }

    private boolean isCopyStatusFailed(CopyState copyState) {
        return CopyStatus.ABORTED.equals(copyState.getStatus()) || CopyStatus.INVALID.equals(copyState.getStatus());
    }

    public void copyVhdImageIfNecessary(AuthenticatedContext ac, CloudStack stack, Image image, String region, AzureClient client) {
        if (azureImageFormatValidator.isMarketplaceImageFormat(image)) {
            LOGGER.info("Skipping image copy as target image ({}) is an Azure Marketplace image!", image.getImageName());
            return;
        }
        CloudContext cloudContext = ac.getCloudContext();
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        String imageStorageName = armStorage.getImageStorageName(new AzureCredentialView(ac.getCloudCredential()), cloudContext, stack);
        String imageResourceGroupName = azureResourceGroupMetadataProvider.getImageResourceGroupName(cloudContext, stack);

        AzureImageInfo azureImageInfo = azureImageInfoService.getImageInfo(imageResourceGroupName, image.getImageName(), ac, client);
        Optional<AzureImage> foundImage = azureImageService.findImage(azureImageInfo, client, ac);
        if (foundImage.isPresent()) {
            LOGGER.info("Custom image with id {} already exists in the target resource group {}, bypassing VHD check!",
                    foundImage.get().getId(), imageResourceGroupName);
            return;
        }

        createResourceGroupIfNotExists(client, resourceGroupName, region, stack);
        createResourceGroupIfNotExists(client, imageResourceGroupName, region, stack);
        azureStorageAccountService.createStorageAccount(ac, client, imageResourceGroupName, imageStorageName, region, stack);
        azureStorageAccountService.createContainerInStorage(client, imageResourceGroupName, imageStorageName);
        if (!storageContainsImage(client, imageResourceGroupName, imageStorageName, azureImageInfo.getImageName())) {
            try {
                LOGGER.info("Starting to copy image: {}, into storage account: {}", image.getImageName(), imageStorageName);
                client.copyImageBlobInStorageContainer(
                        imageResourceGroupName, imageStorageName, IMAGES_CONTAINER, image.getImageName(), azureImageInfo.getImageName());
            } catch (CloudConnectorException e) {
                LOGGER.warn("Something happened during start image copy.", e);
            }
        } else {
            LOGGER.info("The image already exists in the storage account.");
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
        List<ListBlobItem> listBlobItems = client.listBlobInStorage(resourceGroupName, storageName, IMAGES_CONTAINER);
        for (ListBlobItem listBlobItem : listBlobItems) {
            if (customVMImageNameProvider.getImageNameFromConnectionString(listBlobItem.getUri().getPath()).equals(imageName)) {
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
