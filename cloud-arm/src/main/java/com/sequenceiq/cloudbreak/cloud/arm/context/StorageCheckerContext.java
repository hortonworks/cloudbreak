package com.sequenceiq.cloudbreak.cloud.arm.context;

import com.sequenceiq.cloudbreak.cloud.arm.task.ArmStorageStatusCheckerTask.StorageStatus;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;

public class StorageCheckerContext extends GroupNameContext {

    private String storageName;

    private StorageStatus expectedStatus;

    public StorageCheckerContext(ArmCredentialView armCredentialView, String groupName, String storageName,
            StorageStatus expectedStatus) {
        super(armCredentialView, groupName);
        this.storageName = storageName;
        this.expectedStatus = expectedStatus;
    }

    public String getStorageName() {
        return storageName;
    }

    public StorageStatus getExpectedStatus() {
        return expectedStatus;
    }
}
