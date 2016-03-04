package com.sequenceiq.cloudbreak.cloud.arm.view;

import com.sequenceiq.cloudbreak.cloud.arm.ArmStorage;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;

public class ArmStorageView {

    private ArmCredentialView acv;

    private CloudContext cloudContext;

    private ArmStorage armStorage;

    public ArmStorageView(ArmCredentialView acv, CloudContext cloudContext, ArmStorage armStorage) {
        this.acv = acv;
        this.cloudContext = cloudContext;
        this.armStorage = armStorage;
    }

    public String getAttachedDiskStorageName(Long vmId) {
        return armStorage.getAttachedDiskStorageName(acv, vmId, cloudContext);
    }
}
