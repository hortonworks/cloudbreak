package com.sequenceiq.cloudbreak.cloud.arm.view

import java.util.ArrayList
import java.util.HashMap

import com.sequenceiq.cloudbreak.cloud.arm.ArmDiskType
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate

class ArmStackView(groupList: List<Group>, armStorageView: ArmStorageView) {

    private val groups = HashMap<String, List<ArmInstanceView>>()

    init {
        for (group in groupList) {
            val groupName = group.type.name
            var existingInstances: MutableList<ArmInstanceView>? = groups[groupName]
            if (existingInstances == null) {
                existingInstances = ArrayList<ArmInstanceView>()
                groups.put(groupName, existingInstances)
            }
            for (instance in group.instances) {
                val template = instance.template
                val attachedDiskStorageName = armStorageView.getAttachedDiskStorageName(template)
                val azureInstance = ArmInstanceView(template, group.type, attachedDiskStorageName, template.volumeType)
                existingInstances.add(azureInstance)
            }
        }
    }

    fun getGroups(): Map<String, List<ArmInstanceView>> {
        return groups
    }

    val storageAccounts: Map<String, ArmDiskType>
        get() {
            val storageAccounts = HashMap<String, ArmDiskType>()
            for (list in getGroups().values) {
                for (armInstanceView in list) {
                    storageAccounts.put(armInstanceView.attachedDiskStorageName, ArmDiskType.getByValue(armInstanceView.attachedDiskStorageType))
                }
            }
            return storageAccounts
        }
}