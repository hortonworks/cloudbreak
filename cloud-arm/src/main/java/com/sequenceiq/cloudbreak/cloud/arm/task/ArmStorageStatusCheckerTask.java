package com.sequenceiq.cloudbreak.cloud.arm.task;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.arm.ArmClient;
import com.sequenceiq.cloudbreak.cloud.arm.context.StorageCheckerContext;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

public class ArmStorageStatusCheckerTask extends PollBooleanStateTask {

    private StorageCheckerContext storageCheckerContext;
    private ArmClient armClient;

    public ArmStorageStatusCheckerTask(AuthenticatedContext authenticatedContext, ArmClient armClient, StorageCheckerContext storageCheckerContext) {
        super(authenticatedContext, true);
        this.armClient = armClient;
        this.storageCheckerContext = storageCheckerContext;
    }

    @Override
    public Boolean call() {
        AzureRMClient client = armClient.createAccess(storageCheckerContext.getArmCredentialView());
        try {
            String storageStatus = client.getStorageStatus(storageCheckerContext.getGroupName(), storageCheckerContext.getStorageName());
            if ("Succeeded".equals(storageStatus)) {
                return true;
            }
        } catch (Exception ex) {
            return false;
        }
        return false;
    }

}
