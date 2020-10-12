package com.sequenceiq.cloudbreak.cloud.azure.task.storageaccount;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.microsoft.azure.management.storage.StorageAccount;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

@Component(StorageAccountChecker.NAME)
@Scope("prototype")
public class StorageAccountChecker extends PollBooleanStateTask {

    public static final String NAME = "StorageAccountChecker";

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageAccountChecker.class);

    private final StorageAccountCheckerContext context;

    public StorageAccountChecker(AuthenticatedContext authenticatedContext, StorageAccountCheckerContext context) {
        super(authenticatedContext, false);
        this.context = context;
    }

    @Override
    protected Boolean doCall() {
        LOGGER.info("Waiting for storage account to be created: {}", context.getStorageAccountName());
        AzureClient client = context.getAzureClient();
        StorageAccount storageAccount = client.getStorageAccountByGroup(context.getResourceGroupName(), context.getStorageAccountName());
        if (storageAccount == null) {
            LOGGER.info("Storage account creation not finished yet");
            return false;
        } else {
            LOGGER.info("Storage account creation has been finished");
            return true;
        }
    }
}
