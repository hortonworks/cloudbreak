package com.sequenceiq.cloudbreak.cloud.arm;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.ListBlobItem;
import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.arm.context.ImageCheckerContext;
import com.sequenceiq.cloudbreak.cloud.arm.context.StorageCheckerContext;
import com.sequenceiq.cloudbreak.cloud.arm.task.ArmImageCopyStatusCheckerTask;
import com.sequenceiq.cloudbreak.cloud.arm.task.ArmStorageStatusCheckerTask;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.domain.CloudRegion;

import groovyx.net.http.HttpResponseException;

@Component
public class ArmSetup implements Setup {

    public static final String VHDS = "vhds";
    public static final String IMAGES = "images";

    private static final Logger LOGGER = LoggerFactory.getLogger(ArmSetup.class);
    private static final String LOCALLY_REDUNDANT_STORAGE = "Standard_LRS";

    @Inject
    private ArmClient armClient;
    @Inject
    private SyncPollingScheduler<Boolean> syncPollingScheduler;
    @Inject
    private ArmTemplateUtils armTemplateUtils;

    @Override
    public void execute(AuthenticatedContext authenticatedContext, CloudStack stack) {
        checkResourceGroups(authenticatedContext);
        String osStorageName = armClient.getStorageName(authenticatedContext.getCloudContext());
        String storageGroup = armTemplateUtils.getStackName(authenticatedContext.getCloudContext());
        AzureRMClient client = armClient.createAccess(authenticatedContext.getCloudCredential());
        try {
            if (!resourceGroupExist(client, storageGroup)) {
                client.createResourceGroup(storageGroup, CloudRegion.valueOf(stack.getRegion()).value());
            }
            if (!storageAccountExist(client, osStorageName)) {
                client.createStorageAccount(storageGroup, osStorageName, CloudRegion.valueOf(stack.getRegion()).value(), LOCALLY_REDUNDANT_STORAGE);
                PollTask<Boolean> task = new ArmStorageStatusCheckerTask(authenticatedContext, armClient,
                        new StorageCheckerContext(new ArmCredentialView(authenticatedContext.getCloudCredential()), storageGroup, osStorageName));
                syncPollingScheduler.schedule(task);
            }
            client.createContainerInStorage(storageGroup, osStorageName, IMAGES);
            if (!storageContainsImage(client, storageGroup, osStorageName, stack.getImage().getImageName())) {
                client.copyImageBlobInStorageContainer(storageGroup, osStorageName, IMAGES, stack.getImage().getImageName());
                PollTask<Boolean> task = new ArmImageCopyStatusCheckerTask(authenticatedContext, armClient,
                        new ImageCheckerContext(new ArmCredentialView(authenticatedContext.getCloudCredential()), storageGroup, osStorageName,
                                IMAGES, stack.getImage().getImageName()));

                syncPollingScheduler.schedule(task);
            }
        } catch (HttpResponseException ex) {
            throw new CloudConnectorException(ex.getResponse().getData().toString(), ex);
        } catch (Exception ex) {
            throw new CloudConnectorException(ex);
        }

        LOGGER.debug("setup has been executed");
    }

    private void checkResourceGroups(AuthenticatedContext authenticatedContext) {
        AzureRMClient client = armClient.createAccess(authenticatedContext.getCloudCredential());
        try {
            client.getResourceGroups();
        } catch (HttpResponseException ex) {
            throw new CloudConnectorException(ex.getResponse().getData().toString(), ex);
        } catch (Exception e) {
            throw new CloudConnectorException("Could not authenticate to azure", e);
        }
        LOGGER.debug("preCheck has been executed");
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
