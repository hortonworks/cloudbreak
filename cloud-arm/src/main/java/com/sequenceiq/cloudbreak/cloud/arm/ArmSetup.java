package com.sequenceiq.cloudbreak.cloud.arm;

import static com.sequenceiq.cloudbreak.cloud.arm.ArmUtils.NOT_FOUND;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CopyState;
import com.microsoft.azure.storage.blob.CopyStatus;
import com.microsoft.azure.storage.blob.ListBlobItem;
import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.arm.context.StorageCheckerContext;
import com.sequenceiq.cloudbreak.cloud.arm.task.ArmPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.common.type.CloudRegion;
import com.sequenceiq.cloudbreak.common.type.ImageStatus;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

import groovyx.net.http.HttpResponseException;

@Component
public class ArmSetup implements Setup {

    public static final String IMAGES = "images";

    private static final Logger LOGGER = LoggerFactory.getLogger(ArmSetup.class);
    private static final String LOCALLY_REDUNDANT_STORAGE = "Standard_LRS";

    @Inject
    private ArmClient armClient;
    @Inject
    private SyncPollingScheduler<Boolean> syncPollingScheduler;
    @Inject
    private ArmUtils armUtils;
    @Inject
    private ResourceNotifier resourceNotifier;
    @Inject
    private ArmPollTaskFactory armPollTaskFactory;

    @Override
    public void prepareImage(AuthenticatedContext authenticatedContext, CloudStack stack) {
        String storageName = armUtils.getStorageName(authenticatedContext.getCloudCredential(), authenticatedContext.getCloudContext(), stack.getRegion());
        String imageResourceGroupName = armUtils.getImageResourceGroupName(authenticatedContext.getCloudContext());
        AzureRMClient client = armClient.createAccess(authenticatedContext.getCloudCredential());
        String region = stack.getRegion();
        try {
            if (!resourceGroupExist(client, imageResourceGroupName)) {
                client.createResourceGroup(imageResourceGroupName, CloudRegion.valueOf(region).value());
            }
            createStorage(authenticatedContext, client, storageName, imageResourceGroupName, region);
            if (!storageContainsImage(client, imageResourceGroupName, storageName, stack.getImage().getImageName())) {
                client.copyImageBlobInStorageContainer(imageResourceGroupName, storageName, IMAGES, stack.getImage().getImageName());
            }
        } catch (HttpResponseException ex) {
            throw new CloudConnectorException(ex.getResponse().getData().toString(), ex);
        } catch (Exception ex) {
            throw new CloudConnectorException(ex);
        }
        LOGGER.debug("prepare image has been executed");
    }

    @Override
    public ImageStatusResult checkImageStatus(AuthenticatedContext authenticatedContext, CloudStack stack) {
        String storageName = armUtils.getStorageName(authenticatedContext.getCloudCredential(), authenticatedContext.getCloudContext(), stack.getRegion());
        String imageResourceGroupName = armUtils.getImageResourceGroupName(authenticatedContext.getCloudContext());
        ArmCredentialView armCredentialView = new ArmCredentialView(authenticatedContext.getCloudCredential());
        AzureRMClient client = armClient.createAccess(armCredentialView);
        try {
            CopyState copyState = client.getCopyStatus(imageResourceGroupName, storageName, IMAGES, stack.getImage().getImageName());
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
    public void execute(AuthenticatedContext authenticatedContext, CloudStack stack) {
        String storageGroup = armUtils.getResourceGroupName(authenticatedContext.getCloudContext());
        AzureRMClient client = armClient.createAccess(authenticatedContext.getCloudCredential());
        CloudResource cloudResource = new CloudResource.Builder().type(ResourceType.ARM_TEMPLATE).name(storageGroup).build();
        String region = stack.getRegion();
        try {
            resourceNotifier.notifyAllocation(cloudResource, authenticatedContext.getCloudContext());
            if (!resourceGroupExist(client, storageGroup)) {
                client.createResourceGroup(storageGroup, CloudRegion.valueOf(region).value());
            }
        } catch (HttpResponseException ex) {
            throw new CloudConnectorException(ex.getResponse().getData().toString(), ex);
        } catch (Exception ex) {
            throw new CloudConnectorException(ex);
        }
        LOGGER.debug("setup has been executed");
    }

    private void createStorage(AuthenticatedContext authenticatedContext, AzureRMClient client, String osStorageName, String storageGroup, String region)
            throws Exception {
        if (!storageAccountExist(client, osStorageName)) {
            client.createStorageAccount(storageGroup, osStorageName, CloudRegion.valueOf(region).value(), LOCALLY_REDUNDANT_STORAGE);
            PollTask<Boolean> task = armPollTaskFactory.newStorageStatusCheckerTask(authenticatedContext, armClient,
                    new StorageCheckerContext(new ArmCredentialView(authenticatedContext.getCloudCredential()), storageGroup, osStorageName));
            syncPollingScheduler.schedule(task);
        }
        client.createContainerInStorage(storageGroup, osStorageName, IMAGES);
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
            return false;
        }
        return false;
    }

    private boolean storageAccountExist(AzureRMClient client, String storageName) {
        try {
            List<Map<String, Object>> storageAccounts = client.getStorageAccounts();
            for (Map<String, Object> stringObjectMap : storageAccounts) {
                if (stringObjectMap.get("name").equals(storageName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private boolean storageContainsImage(AzureRMClient client, String groupName, String storageName, String image) throws URISyntaxException, StorageException {
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
