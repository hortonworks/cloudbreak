package com.sequenceiq.cloudbreak.cloud.arm;

import static com.sequenceiq.cloudbreak.cloud.arm.ArmStorage.IMAGES;
import static com.sequenceiq.cloudbreak.cloud.arm.ArmUtils.NOT_FOUND;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CopyState;
import com.microsoft.azure.storage.blob.CopyStatus;
import com.microsoft.azure.storage.blob.ListBlobItem;
import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.api.model.WasbFileSystemConfiguration;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.arm.task.ArmPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.FileSystem;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.common.type.ImageStatus;
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

import groovyx.net.http.HttpResponseException;

@Component
public class ArmSetup implements Setup {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArmSetup.class);
    private static final String TEST_CONTAINER = "cb-test-container";
    private static final String DASH = "DASH";

    @Inject
    private ArmClient armClient;
    @Inject
    private SyncPollingScheduler<Boolean> syncPollingScheduler;
    @Inject
    private ArmUtils armUtils;
    @Inject
    private ArmPollTaskFactory armPollTaskFactory;
    @Inject
    private ArmStorage armStorage;

    @Override
    public void prepareImage(AuthenticatedContext ac, CloudStack stack, Image image) {
        LOGGER.info("prepare image: {}", image);
        ArmCredentialView acv = new ArmCredentialView(ac.getCloudCredential());
        String imageStorageName = armStorage.getImageStorageName(acv, ac.getCloudContext(), armStorage.getPersistentStorageName(stack.getParameters()),
                armStorage.getArmAttachedStorageOption(stack.getParameters()));
        String resourceGroupName = armUtils.getResourceGroupName(ac.getCloudContext());
        String imageResourceGroupName = armStorage.getImageResourceGroupName(ac.getCloudContext(), stack.getParameters());
        AzureRMClient client = armClient.getClient(ac.getCloudCredential());
        String region = ac.getCloudContext().getLocation().getRegion().value();
        try {
            if (!resourceGroupExist(client, resourceGroupName)) {
                client.createResourceGroup(resourceGroupName, region);
            }
            if (!resourceGroupExist(client, imageResourceGroupName)) {
                client.createResourceGroup(imageResourceGroupName, region);
            }
            armStorage.createStorage(ac, client, imageStorageName, ArmDiskType.LOCALLY_REDUNDANT, imageResourceGroupName, region);
            client.createContainerInStorage(imageResourceGroupName, imageStorageName, IMAGES);
            if (!storageContainsImage(client, imageResourceGroupName, imageStorageName, image.getImageName())) {
                client.copyImageBlobInStorageContainer(imageResourceGroupName, imageStorageName, IMAGES, image.getImageName());
            }
        } catch (HttpResponseException ex) {
            throw new CloudConnectorException(ex.getResponse().getData().toString(), ex);
        } catch (Exception ex) {
            throw new CloudConnectorException(ex);
        }
        LOGGER.debug("prepare image has been executed");
    }

    @Override
    public ImageStatusResult checkImageStatus(AuthenticatedContext ac, CloudStack stack, Image image) {
        ArmCredentialView acv = new ArmCredentialView(ac.getCloudCredential());
        String imageStorageName = armStorage.getImageStorageName(acv, ac.getCloudContext(), armStorage.getPersistentStorageName(stack.getParameters()),
                armStorage.getArmAttachedStorageOption(stack.getParameters()));
        String imageResourceGroupName = armStorage.getImageResourceGroupName(ac.getCloudContext(), stack.getParameters());
        ArmCredentialView armCredentialView = new ArmCredentialView(ac.getCloudCredential());
        AzureRMClient client = armClient.getClient(armCredentialView);
        try {
            CopyState copyState = client.getCopyStatus(imageResourceGroupName, imageStorageName, IMAGES, image.getImageName());
            if (CopyStatus.SUCCESS.equals(copyState.getStatus())) {
                return new ImageStatusResult(ImageStatus.CREATE_FINISHED, ImageStatusResult.COMPLETED);
            } else if (CopyStatus.ABORTED.equals(copyState.getStatus()) || CopyStatus.INVALID.equals(copyState.getStatus())) {
                return new ImageStatusResult(ImageStatus.CREATE_FAILED, 0);
            } else {
                int percentage = (int) (((double) copyState.getBytesCopied() * ImageStatusResult.COMPLETED) / (double) copyState.getTotalBytes());
                LOGGER.info(String.format("CopyStatus Pending %s byte/%s byte: %.4s %%", copyState.getTotalBytes(), copyState.getBytesCopied(), percentage));
                return new ImageStatusResult(ImageStatus.IN_PROGRESS, percentage);
            }
        } catch (HttpResponseException e) {
            if (e.getStatusCode() != NOT_FOUND) {
                throw new CloudConnectorException(e.getResponse().getData().toString());
            } else {
                return new ImageStatusResult(ImageStatus.IN_PROGRESS, ImageStatusResult.HALF);
            }
        } catch (Exception ex) {
            return new ImageStatusResult(ImageStatus.IN_PROGRESS, ImageStatusResult.HALF);
        }
    }

    @Override
    public void prerequisites(AuthenticatedContext ac, CloudStack stack, PersistenceNotifier persistenceNotifier) {
        String storageGroup = armUtils.getResourceGroupName(ac.getCloudContext());
        AzureRMClient client = armClient.getClient(ac.getCloudCredential());
        CloudResource cloudResource = new CloudResource.Builder().type(ResourceType.ARM_TEMPLATE).name(storageGroup).build();
        String region = ac.getCloudContext().getLocation().getRegion().value();
        try {
            persistenceNotifier.notifyAllocation(cloudResource, ac.getCloudContext());
            if (!resourceGroupExist(client, storageGroup)) {
                client.createResourceGroup(storageGroup, region);
            }
        } catch (HttpResponseException ex) {
            throw new CloudConnectorException(ex.getResponse().getData().toString(), ex);
        } catch (Exception ex) {
            throw new CloudConnectorException(ex);
        }
        LOGGER.debug("setup has been executed");
    }

    @Override
    public void validateFileSystem(FileSystem fileSystem) throws Exception {
        String fileSystemType = fileSystem.getType();
        String accountName = fileSystem.getParameter(WasbFileSystemConfiguration.ACCOUNT_NAME, String.class);
        String accountKey = fileSystem.getParameter(WasbFileSystemConfiguration.ACCOUNT_KEY, String.class);
        String connectionString = "DefaultEndpointsProtocol=https;AccountName=" + accountName + ";AccountKey=" + accountKey;
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(connectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer containerReference = blobClient.getContainerReference(TEST_CONTAINER + System.nanoTime());
        try {
            containerReference.createIfNotExists();
            containerReference.delete();
            if (DASH.equals(fileSystemType)) {
                throw new CloudConnectorException("The provided account belongs to a single storage account, but the selected file system is WASB with DASH");
            }
        } catch (StorageException e) {
            if (!DASH.equals(fileSystemType) && e.getCause() instanceof UnknownHostException) {
                throw new CloudConnectorException("The provided account does not belong to a valid storage account");
            }
        }
    }

    private boolean resourceGroupExist(AzureRMClient client, String groupName) {
        try {
            List<Map<String, Object>> resourceGroups = client.getResourceGroups();
            for (Map<String, Object> resourceGroup : resourceGroups) {
                if (resourceGroup.get("name").equals(groupName)) {
                    return true;
                }
            }

        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    private boolean storageContainsImage(AzureRMClient client, String groupName, String storageName, String image) {
        List<ListBlobItem> listBlobItems = client.listBlobInStorage(groupName, storageName, IMAGES);
        for (ListBlobItem listBlobItem : listBlobItems) {
            if (getNameFromConnectionString(listBlobItem.getUri().getPath()).equals(image.split("/")[image.split("/").length - 1])) {
                return true;
            }
        }
        return false;
    }

    private String getNameFromConnectionString(String connection) {
        return connection.split("/")[connection.split("/").length - 1];
    }

}
