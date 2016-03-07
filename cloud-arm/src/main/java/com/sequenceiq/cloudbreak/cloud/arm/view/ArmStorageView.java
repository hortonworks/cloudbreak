package com.sequenceiq.cloudbreak.cloud.arm.view;

import com.sequenceiq.cloudbreak.cloud.arm.ArmDiskType;
import com.sequenceiq.cloudbreak.cloud.arm.ArmStorage;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

public class ArmStorageView {

    private ArmCredentialView acv;

    private CloudContext cloudContext;

    private ArmStorage armStorage;

    public ArmStorageView(ArmCredentialView acv, CloudContext cloudContext, ArmStorage armStorage) {
        this.acv = acv;
        this.cloudContext = cloudContext;
        this.armStorage = armStorage;
    }

    public String getAttachedDiskStorageName(InstanceTemplate template) {
        return armStorage.getAttachedDiskStorageName(acv, template.getPrivateId(), cloudContext, ArmDiskType.getByValue(template.getVolumeType()));
    }
}
