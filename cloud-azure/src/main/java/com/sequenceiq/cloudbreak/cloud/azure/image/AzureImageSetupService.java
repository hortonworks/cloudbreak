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
        String imageResourceGroupName = azureResourceGroupMetadataProvider.getImageResourceGroupName(ac.getCloudContext(), stack);
        AzureClient client = ac.getParameter(AzureClient.class);

        AzureCredentialView acv = new AzureCredentialView(ac.getCloudCredential());
        String imageStorageName = armStorage.getImageStorageName(acv, ac.getCloudContext(), stack);
        try {
            CopyState copyState = client.getCopyStatus(imageResourceGroupName, imageStorageName, IMAGES_CONTAINER, image.getImageName());
            if (CopyStatus.SUCCESS.equals(copyState.getStatus())) {
                if (StringUtils.isEmpty(armStorage.getCustomImageId(client, ac, stack))) {
                    LOGGER.error("The image is not found in the storage.");
                    return new ImageStatusResult(ImageStatus.CREATE_FAILED, ImageStatusResult.COMPLETED);
                }
                LOGGER.info("The image copy has been finished.");
                return new ImageStatusResult(ImageStatus.CREATE_FINISHED, ImageStatusResult.COMPLETED);
            } else if (CopyStatus.ABORTED.equals(copyState.getStatus()) || CopyStatus.INVALID.equals(copyState.getStatus())) {
                LOGGER.error("The image copy has been failed with status: {}", copyState.getStatus());
                return new ImageStatusResult(ImageStatus.CREATE_FAILED, 0);
            } else {
                int percentage = (int) (((double) copyState.getBytesCopied() * ImageStatusResult.COMPLETED) / copyState.getTotalBytes());
                LOGGER.info("CopyStatus, Total:{} / Pending:{} bytes, {}%", copyState.getTotalBytes(), copyState.getBytesCopied(), percentage);
                return new ImageStatusResult(ImageStatus.IN_PROGRESS, percentage);
            }
        } catch (RuntimeException ex) {
            String msg = String.format("Failed to check the status of the image in resource group '%s', image storage name '%s'",
                    imageResourceGroupName, imageStorageName);
            LOGGER.warn(msg, ex);
            return new ImageStatusResult(ImageStatus.IN_PROGRESS, ImageStatusResult.HALF);
        }
    }

    public void copyVhdImageIfNecessary(AuthenticatedContext ac, CloudStack stack, Image image, String region, AzureClient client) {
        CloudContext cloudContext = ac.getCloudContext();
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        String imageStorageName = armStorage.getImageStorageName(new AzureCredentialView(ac.getCloudCredential()), cloudContext, stack);
        String imageResourceGroupName = azureResourceGroupMetadataProvider.getImageResourceGroupName(cloudContext, stack);

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
            LOGGER.info("The image is already exists in the storage account.");
        }
    }

    private boolean storageContainsImage(AzureClient client, String resourceGroupName, String storageName, String image) {
        List<ListBlobItem> listBlobItems = client.listBlobInStorage(resourceGroupName, storageName, IMAGES_CONTAINER);
        for (ListBlobItem listBlobItem : listBlobItems) {
            if (getNameFromConnectionString(listBlobItem.getUri().getPath()).equals(getNameFromConnectionString(image))) {
                return true;
            }
        }
        return false;
    }

    private String getNameFromConnectionString(String connection) {
        return connection.split("/")[connection.split("/").length - 1];
    }

    private void createResourceGroupIfNotExists(AzureClient client, String resourceGroupName, String region, CloudStack stack) {
        if (!client.resourceGroupExists(resourceGroupName)) {
            LOGGER.info("Creating resource group: {}", resourceGroupName);
            client.createResourceGroup(resourceGroupName, region, stack.getTags().getAll());
        }
    }

}
