package com.sequenceiq.cloudbreak.cloud.arm.task;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.BooleanStateConnector;
import com.sequenceiq.cloudbreak.cloud.arm.ArmClient;
import com.sequenceiq.cloudbreak.cloud.arm.context.StorageCheckerContext;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;

public class ArmStorageStatusCheckerTask implements BooleanStateConnector {

    private StorageCheckerContext storageCheckerContext;
    private ArmClient armClient;

    public ArmStorageStatusCheckerTask(ArmClient armClient, StorageCheckerContext storageCheckerContext) {
        this.armClient = armClient;
        this.storageCheckerContext = storageCheckerContext;
    }

    @Override
    public Boolean check(AuthenticatedContext authenticatedContext) {
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
