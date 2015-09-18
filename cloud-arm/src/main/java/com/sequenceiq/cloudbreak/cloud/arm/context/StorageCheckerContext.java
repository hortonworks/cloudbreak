package com.sequenceiq.cloudbreak.cloud.arm.context;

import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;

public class StorageCheckerContext extends ArmStatusCheckerContext {

    private String groupName;
    private String storageName;

    public StorageCheckerContext(ArmCredentialView armCredentialView, String groupName, String storageName) {
        super(armCredentialView);
        this.groupName = groupName;
        this.storageName = storageName;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getStorageName() {
        return storageName;
    }
}
