package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType.LOCALLY_REDUNDANT;
import static com.sequenceiq.cloudbreak.cloud.azure.AzureStorage.IMAGES_CONTAINER;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.storage.StorageAccount;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.task.AzurePollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.azure.task.storageaccount.StorageAccountCheckerContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.common.api.tag.model.Tags;

@Service
public class AzureStorageAccountService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureStorageAccountService.class);

    private static final int STORAGE_ACCOUNT_CREATION_CHECKING_INTERVAL = 1000;

    private static final int STORAGE_ACCOUNT_CREATION_CHECKING_MAX_ATTEMPT = 30;

    private static final int MAX_FAILURE_TOLERANT = 5;

    @Inject
    private AzurePollTaskFactory azurePollTaskFactory;

    @Inject
    private SyncPollingScheduler syncPollingScheduler;

    @Inject
    private AzureStorage armStorage;

    public void createStorageAccount(AuthenticatedContext ac, AzureClient client, String resourceGroup, String storageName, String region, CloudStack stack) {
        StorageAccount storageAccount = client.getStorageAccountByGroup(resourceGroup, storageName);
        if (storageAccount == null) {
            try {
                LOGGER.info("Creating storage account: {}", storageName);
                armStorage.createStorage(client, storageName, LOCALLY_REDUNDANT, resourceGroup, region, isEncryptionNeeded(stack),
                        Tags.getAll(stack.getTags()));
                pollStorageAccountCreation(ac, new StorageAccountCheckerContext(client, resourceGroup, storageName));
            } catch (CloudException e) {
                LOGGER.error("Error during storage account creation: {}.", storageName, e);
                throw new CloudConnectorException("Storage account creation failed.", e);
            }
        } else {
            LOGGER.info("Storage account {} already exist in resource group {}", storageName, resourceGroup);
        }
    }

    public void createContainerInStorage(AzureClient client, String resourceGroup, String storageName) {
        LOGGER.info("Creating container: {} in storage storage account: {}", IMAGES_CONTAINER, storageName);
        client.createContainerInStorage(resourceGroup, storageName, IMAGES_CONTAINER);
    }

    private void pollStorageAccountCreation(AuthenticatedContext ac, StorageAccountCheckerContext checkerContext) {
        PollTask<Boolean> storageAccountCreationStatusCheckerTask = azurePollTaskFactory.storageAccountCheckerTask(ac, checkerContext);
        try {
            LOGGER.info("Start polling storage account creation: {}", checkerContext.getStorageAccountName());
            syncPollingScheduler.schedule(storageAccountCreationStatusCheckerTask, STORAGE_ACCOUNT_CREATION_CHECKING_INTERVAL,
                    STORAGE_ACCOUNT_CREATION_CHECKING_MAX_ATTEMPT, MAX_FAILURE_TOLERANT);
        } catch (Exception e) {
            LOGGER.debug("Storage account creation failed.", e);
        }
    }

    private Boolean isEncryptionNeeded(CloudStack stack) {
        return armStorage.isEncrytionNeeded(stack.getParameters());
    }
}
