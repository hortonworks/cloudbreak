package com.sequenceiq.cloudbreak.cloud.openstack.view

import java.util.ArrayList
import java.util.HashMap

import com.fasterxml.jackson.core.JsonProcessingException
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate
import com.sequenceiq.cloudbreak.cloud.model.Volume
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils
import com.sequenceiq.cloudbreak.util.JsonUtil

class NovaInstanceView(private val instance: InstanceTemplate, val type: InstanceGroupType) {

    val flavor: String
        get() = instance.flavor

    val instanceId: String
        get() = instance.groupName.replace("_".toRegex(), "") + "_" + instance.privateId

    val privateId: Long
        get() = instance.privateId!!

    val volumes: List<CinderVolumeView>
        get() {
            val list = ArrayList<CinderVolumeView>()
            var index = 0
            for (volume in instance.volumes) {
                val cv = CinderVolumeView(volume, index)
                list.add(cv)
                index++
            }
            return list
        }

    val metadataMap: Map<String, String>
        get() = generateMetadata()

    val metadata: String
        get() {
            try {
                return JsonUtil.writeValueAsString(generateMetadata())
            } catch (e: JsonProcessingException) {
                return generateMetadata().toString()
            }

        }

    private fun generateMetadata(): Map<String, String> {
        val metadata = HashMap<String, String>()
        metadata.put(OpenStackUtils.CB_INSTANCE_GROUP_NAME, instance.groupName)
        metadata.put(OpenStackUtils.CB_INSTANCE_PRIVATE_ID, java.lang.Long.toString(privateId))
        return metadata
    }

}
