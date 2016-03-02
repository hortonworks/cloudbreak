package com.sequenceiq.cloudbreak.cloud.arm.context;

import com.sequenceiq.cloudbreak.cloud.arm.task.ArmStorageStatusCheckerTask.StorageStatus;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;

public class StorageCheckerContext extends ArmStatusCheckerContext {

    private String groupName;
    private String storageName;
    private StorageStatus expectedStatus;

    public StorageCheckerContext(ArmCredentialView armCredentialView, String groupName, String storageName,
            StorageStatus expectedStatus) {
        super(armCredentialView);
        this.groupName = groupName;
        this.storageName = storageName;
        this.expectedStatus = expectedStatus;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getStorageName() {
        return storageName;
    }

    public StorageStatus getExpectedStatus() {
        return expectedStatus;
    }
}
