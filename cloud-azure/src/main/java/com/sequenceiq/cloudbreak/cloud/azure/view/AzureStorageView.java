package com.sequenceiq.cloudbreak.cloud.azure.view;

import com.sequenceiq.cloudbreak.cloud.azure.ArmAttachedStorageOption;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

public class AzureStorageView {

    private final AzureCredentialView acv;

    private final CloudContext cloudContext;

    private final AzureStorage armStorage;

    private final ArmAttachedStorageOption armAttachedStorageOption;

    public AzureStorageView(AzureCredentialView acv, CloudContext cloudContext, AzureStorage armStorage,
            ArmAttachedStorageOption armAttachedStorageOption) {
        this.acv = acv;
        this.cloudContext = cloudContext;
        this.armStorage = armStorage;
        this.armAttachedStorageOption = armAttachedStorageOption;
    }

    public String getAttachedDiskStorageName(InstanceTemplate template) {
        return armStorage.getAttachedDiskStorageName(armAttachedStorageOption, acv, template.getPrivateId(),
                cloudContext, AzureDiskType.getByValue(template.getVolumes().get(0).getType()));
    }
}
