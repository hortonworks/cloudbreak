package com.sequenceiq.cloudbreak.cloud.azure.view;

import com.sequenceiq.cloudbreak.api.model.ArmAttachedStorageOption;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

public class AzureStorageView {

    private AzureCredentialView acv;

    private CloudContext cloudContext;

    private AzureStorage armStorage;

    private ArmAttachedStorageOption armAttachedStorageOption;

    public AzureStorageView(AzureCredentialView acv, CloudContext cloudContext, AzureStorage armStorage,
            ArmAttachedStorageOption armAttachedStorageOption) {
        this.acv = acv;
        this.cloudContext = cloudContext;
        this.armStorage = armStorage;
        this.armAttachedStorageOption = armAttachedStorageOption;
    }

    public String getAttachedDiskStorageName(InstanceTemplate template) {
        return armStorage.getAttachedDiskStorageName(armAttachedStorageOption, acv, template.getPrivateId(),
                cloudContext, AzureDiskType.getByValue(template.getVolumeType()));
    }
}
