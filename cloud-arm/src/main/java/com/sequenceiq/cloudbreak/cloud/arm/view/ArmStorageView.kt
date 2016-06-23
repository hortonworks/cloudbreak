package com.sequenceiq.cloudbreak.cloud.arm.view

import com.sequenceiq.cloudbreak.cloud.arm.ArmDiskType
import com.sequenceiq.cloudbreak.api.model.ArmAttachedStorageOption
import com.sequenceiq.cloudbreak.cloud.arm.ArmStorage
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate

class ArmStorageView(private val acv: ArmCredentialView, private val cloudContext: CloudContext, private val armStorage: ArmStorage,
                     private val armAttachedStorageOption: ArmAttachedStorageOption) {

    fun getAttachedDiskStorageName(template: InstanceTemplate): String {
        return armStorage.getAttachedDiskStorageName(armAttachedStorageOption, acv, template.privateId,
                cloudContext, ArmDiskType.getByValue(template.volumeType))
    }
}
