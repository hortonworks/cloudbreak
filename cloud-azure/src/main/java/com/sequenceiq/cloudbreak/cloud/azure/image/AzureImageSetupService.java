package com.sequenceiq.cloudbreak.cloud.azure.image;

import static com.sequenceiq.cloudbreak.cloud.azure.AzureStorage.IMAGES_CONTAINER;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.microsoft.azure.storage.blob.CopyState;
import com.microsoft.azure.storage.blob.CopyStatus;
import com.microsoft.azure.storage.blob.ListBlobItem;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorageAccountService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
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

    public ImageStatusResult checkImageStatus(AuthenticatedContext ac, CloudStack stack, Image image) {
        CloudContext cloudContext = ac.getCloudContext();
        String imageResourceGroupName = azureResourceGroupMetadataProvider.getImageResourceGroupName(cloudContext, stack);
        AzureClient client = ac.getParameter(AzureClient.class);

        String customImageId = client.getCustomImageId(imageResourceGroupName, image.getImageName(), cloudContext.getLocation().getRegion().getRegionName(),
                false).getId();
        if (!StringUtils.isEmpty(customImageId)) {
            LOGGER.info("Custom image with id {} already exists in the target resource group {}, bypassing VHD copy check!",
                    customImageId, imageResourceGroupName);
            return new ImageStatusResult(ImageStatus.CREATE_FINISHED, ImageStatusResult.COMPLETED);
        }
        AzureCredentialView acv = new AzureCredentialView(ac.getCloudCredential());
        String imageStorageName = armStorage.getImageStorageName(acv, cloudContext, stack);
        try {
            CopyState copyState = client.getCopyStatus(imageResourceGroupName, imageStorageName, IMAGES_CONTAINER, image.getImageName());
            if (copyState == null && storageContainsImage(client, imageResourceGroupName, imageStorageName, image.getImageName())) {
                LOGGER.debug("The copy has been finished because the storage account already contains the image.");
                return new ImageStatusResult(ImageStatus.CREATE_FINISHED, ImageStatusResult.COMPLETED);
            } else if (copyState == null && !storageContainsImage(client, imageResourceGroupName, imageStorageName, image.getImageName())) {
                throw new CloudConnectorException(
                        "Image copy failed because the copy state is not available and the storage account does not contains the image.");
            }
            if (CopyStatus.SUCCESS.equals(copyState.getStatus())) {
                if (!storageContainsImage(client, imageResourceGroupName, imageStorageName, image.getImageName())) {
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
        } catch (RuntimeException ex) {
            String msg = String.format("Failed to check the status of the image in resource group '%s', image storage name '%s'",
                    imageResourceGroupName, imageStorageName);
            LOGGER.error(msg, ex);
            return new ImageStatusResult(ImageStatus.CREATE_FAILED, ImageStatusResult.INIT);
        }
    }

    private boolean isCopyStatusFailed(CopyState copyState) {
        return CopyStatus.ABORTED.equals(copyState.getStatus()) || CopyStatus.INVALID.equals(copyState.getStatus());
    }

    public void copyVhdImageIfNecessary(AuthenticatedContext ac, CloudStack stack, Image image, String region, AzureClient client) {
        CloudContext cloudContext = ac.getCloudContext();
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        String imageStorageName = armStorage.getImageStorageName(new AzureCredentialView(ac.getCloudCredential()), cloudContext, stack);
        String imageResourceGroupName = azureResourceGroupMetadataProvider.getImageResourceGroupName(cloudContext, stack);

        String customImageId = client.getCustomImageId(resourceGroupName, image.getImageName(), region, false).getId();
        if (!StringUtils.isEmpty(customImageId)) {
            LOGGER.info("Custom image with id {} already exists in the target resource group {}, bypassing VHD check!", customImageId, resourceGroupName);
            return;
        }

        createResourceGroupIfNotExists(client, resourceGroupName, region, stack);
        createResourceGroupIfNotExists(client, imageResourceGroupName, region, stack);
        azureStorageAccountService.createStorageAccount(ac, client, imageResourceGroupName, imageStorageName, region, stack);
        azureStorageAccountService.createContainerInStorage(client, imageResourceGroupName, imageStorageName);
        if (!storageContainsImage(client, imageResourceGroupName, imageStorageName, image.getImageName())) {
            try {
                LOGGER.info("Starting to copy image: {}, into storage account: {}", image.getImageName(), imageStorageName);
                client.copyImageBlobInStorageContainer(imageResourceGroupName, imageStorageName, IMAGES_CONTAINER, image.getImageName());
            } catch (CloudConnectorException e) {
                LOGGER.warn("Something happened during start image copy.", e);
            }
        } else {
            LOGGER.info("The image already exists in the storage account.");
        }
    }

    private boolean storageContainsImage(AzureClient client, String resourceGroupName, String storageName, String image) {
        List<ListBlobItem> listBlobItems = client.listBlobInStorage(resourceGroupName, storageName, IMAGES_CONTAINER);
        for (ListBlobItem listBlobItem : listBlobItems) {
            if (getNameFromConnectionString(listBlobItem.getUri().getPath()).equals(getNameFromConnectionString(image))) {
                LOGGER.info("The storage account {} in {} resource group contains the requested image {}", storageName, resourceGroupName, image);
                return true;
            }
        }
        LOGGER.info("The storage account {} in {} resource group does not contains the requested image {}", storageName, resourceGroupName, image);
        return false;
    }

    private String getNameFromConnectionString(String connection) {
        return connection.split("/")[connection.split("/").length - 1];
    }

    private void createResourceGroupIfNotExists(AzureClient client, String resourceGroupName, String region, CloudStack stack) {
        if (!client.resourceGroupExists(resourceGroupName)) {
            LOGGER.info("Creating resource group: {}", resourceGroupName);
            client.createResourceGroup(resourceGroupName, region, stack.getTags());
        }
    }

}
