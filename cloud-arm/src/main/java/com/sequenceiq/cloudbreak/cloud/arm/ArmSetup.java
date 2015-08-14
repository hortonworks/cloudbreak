package com.sequenceiq.cloudbreak.cloud.arm;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.ListBlobItem;
import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.arm.poller.ArmImageCopyStatusCheckerTask;
import com.sequenceiq.cloudbreak.cloud.arm.poller.ArmStorageStatusCheckerTask;
import com.sequenceiq.cloudbreak.cloud.arm.poller.context.ImageCheckerContext;
import com.sequenceiq.cloudbreak.cloud.arm.poller.context.StorageCheckerContext;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.BooleanResult;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.transform.ResourcesStatePollerResults;
import com.sequenceiq.cloudbreak.domain.CloudRegion;

@Component
public class ArmSetup implements Setup {

    public static final String IMAGES = "images";
    public static final String VHDS = "vhds";

    private static final int ARM_POLLING_INTERVAL = 5000;
    private static final int MAX_ATTEMPTS_FOR_HOSTS = 240;
    private static final int MAX_FAILURE_COUNT = 5;

    @Inject
    private ArmClient armClient;

    @Inject
    private SyncPollingScheduler<BooleanResult> syncPollingScheduler;
    @Inject
    private PollTaskFactory statusCheckFactory;

    @Override
    public Map<String, Object> execute(AuthenticatedContext authenticatedContext, CloudStack stack) throws Exception {
        String osStorageName = armClient.getStorageName(authenticatedContext.getCloudCredential(), stack.getRegion());
        String storageGroup = armClient.getStorageGroup(authenticatedContext.getCloudCredential(), stack.getRegion());
        AzureRMClient client = armClient.createAccess(authenticatedContext.getCloudCredential());
        try {
            if (!resourceGroupExist(client, storageGroup)) {
                client.createResourceGroup(storageGroup, CloudRegion.valueOf(stack.getRegion()).value());
            }
            if (!storageAccountExist(client, osStorageName)) {
                client.createStorageAccount(storageGroup, osStorageName, CloudRegion.valueOf(stack.getRegion()).value());
                BooleanResult statePollerResult = ResourcesStatePollerResults.transformToFalseBooleanResult(authenticatedContext.getCloudContext());
                PollTask<BooleanResult> task = statusCheckFactory.newPollBooleanStateTask(authenticatedContext,
                        new ArmStorageStatusCheckerTask(armClient,
                                new StorageCheckerContext(new ArmCredentialView(authenticatedContext.getCloudCredential()), storageGroup, osStorageName)));
                if (!task.completed(statePollerResult)) {
                    statePollerResult = syncPollingScheduler.schedule(task);
                }
            }
            client.createContainerInStorage(storageGroup, osStorageName, IMAGES);
            if (!storageContainsImage(client, storageGroup, osStorageName, stack.getImage().getImageName())) {
                client.copyImageBlobInStorageContainer(storageGroup, osStorageName, IMAGES, stack.getImage().getImageName());
                BooleanResult statePollerResult = ResourcesStatePollerResults.transformToFalseBooleanResult(authenticatedContext.getCloudContext());
                PollTask<BooleanResult> task = statusCheckFactory.newPollBooleanStateTask(authenticatedContext,
                        new ArmImageCopyStatusCheckerTask(armClient,
                                new ImageCheckerContext(new ArmCredentialView(authenticatedContext.getCloudCredential()), storageGroup, osStorageName,
                                        IMAGES, stack.getImage().getImageName())));
                if (!task.completed(statePollerResult)) {
                    statePollerResult = syncPollingScheduler.schedule(task);
                }
            }
        } catch (Exception ex) {
            throw new CloudConnectorException(ex);
        }
        return new HashMap<>();
    }

    @Override
    public String preCheck(AuthenticatedContext authenticatedContext, CloudStack stack) {
        return null;
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
