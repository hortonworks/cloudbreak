package com.sequenceiq.cloudbreak.cloud.arm;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.arm.context.StorageCheckerContext;
import com.sequenceiq.cloudbreak.cloud.arm.task.ArmPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@Service
public class ArmStorage {

    public static final String IMAGES = "images";
    public static final String STORAGE_BLOB_PATTERN = "https://%s.blob.core.windows.net/";
    private static final String LOCALLY_REDUNDANT_STORAGE = "Standard_LRS";

    @Inject
    private ArmClient armClient;
    @Inject
    private SyncPollingScheduler<Boolean> syncPollingScheduler;
    @Inject
    private ArmPollTaskFactory armPollTaskFactory;

    public void createStorage(AuthenticatedContext authenticatedContext, AzureRMClient client, String osStorageName, String storageGroup, String region)
            throws Exception {
        if (!storageAccountExist(client, osStorageName)) {
            client.createStorageAccount(storageGroup, osStorageName, region, LOCALLY_REDUNDANT_STORAGE);
            PollTask<Boolean> task = armPollTaskFactory.newStorageStatusCheckerTask(authenticatedContext, armClient,
                    new StorageCheckerContext(new ArmCredentialView(authenticatedContext.getCloudCredential()), storageGroup, osStorageName));
            syncPollingScheduler.schedule(task);
        }
        client.createContainerInStorage(storageGroup, osStorageName, IMAGES);
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
}


