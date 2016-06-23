package com.sequenceiq.cloudbreak.cloud.model

import com.google.common.collect.ImmutableList
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel

class InstanceTemplate(val flavor: String, val groupName: String, val privateId: Long?, volumes: List<Volume>, val status: InstanceStatus, parameters: MutableMap<String, Any>) : DynamicModel(parameters) {
    val volumes: List<Volume>

    init {
        this.volumes = ImmutableList.copyOf(volumes)
    }

    val volumeType: String
        get() = volumes[0].type

    val volumeSize: Int
        get() = volumes[0].size

    override fun toString(): String {
        val sb = StringBuilder("InstanceTemplate{")
        sb.append("flavor='").append(flavor).append('\'')
        sb.append(", groupName='").append(groupName).append('\'')
        sb.append(", privateId=").append(privateId)
        sb.append(", volumes=").append(volumes)
        sb.append(", status=").append(status)
        sb.append('}')
        return sb.toString()
    }
}
