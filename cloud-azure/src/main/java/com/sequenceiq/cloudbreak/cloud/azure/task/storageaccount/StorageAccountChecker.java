package com.sequenceiq.cloudbreak.cloud.azure.task.storageaccount;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.azure.resourcemanager.storage.models.ProvisioningState;
import com.azure.resourcemanager.storage.models.StorageAccount;
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
        Optional<StorageAccount> storageAccount = client.getStorageAccountByGroup(context.getResourceGroupName(), context.getStorageAccountName());
        if (storageAccount.isEmpty() || !ProvisioningState.SUCCEEDED.equals(storageAccount.get().provisioningState())) {
            LOGGER.info("Storage account creation not finished yet");
            return false;
        } else {
            LOGGER.info("Storage account creation has been finished");
            return true;
        }
    }
}
