package com.sequenceiq.cloudbreak.cloud.arm.view

import java.util.ArrayList
import java.util.HashMap

import com.fasterxml.jackson.core.JsonProcessingException
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType
import com.sequenceiq.cloudbreak.cloud.arm.ArmDiskType
import com.sequenceiq.cloudbreak.cloud.arm.ArmStorage
import com.sequenceiq.cloudbreak.cloud.arm.ArmUtils
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate
import com.sequenceiq.cloudbreak.cloud.model.Volume
import com.sequenceiq.cloudbreak.util.JsonUtil

class ArmInstanceView(private val instance: InstanceTemplate, val type: InstanceGroupType, val attachedDiskStorageName: String, val attachedDiskStorageType: String) {

    /**
     * Used in freemarker template.
     */
    val flavor: String
        get() = instance.flavor

    /**
     * Used in freemarker template.
     */
    val isBootDiagnosticsEnabled: Boolean
        get() = ArmDiskType.LOCALLY_REDUNDANT == ArmDiskType.getByValue(instance.volumeType)

    val instanceId: String
        get() = ArmUtils.getGroupName(instance.groupName) + instance.privateId!!

    val privateId: Long
        get() = instance.privateId!!

    val volumes: List<ArmVolumeView>
        get() {
            val list = ArrayList<ArmVolumeView>()
            var index = 0
            for (volume in instance.volumes) {
                val cv = ArmVolumeView(volume, index)
                list.add(cv)
                index++
            }
            return list
        }

    /**
     * Used in freemarker template.
     */
    val attachedDiskStorageUrl: String
        get() = String.format(ArmStorage.STORAGE_BLOB_PATTERN, attachedDiskStorageName)

    val metadata: String
        get() {
            try {
                return JsonUtil.writeValueAsString(generateMetadata())
            } catch (e: JsonProcessingException) {
                return generateMetadata().toString()
            }

        }

    private fun generateMetadata(): Map<String, String> {
        return HashMap()
    }
}