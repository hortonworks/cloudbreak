package com.sequenceiq.cloudbreak.cloud.model

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap

/**
 * Class that describes complete structure of infrastructure that needs to be started on the Cloud Provider
 */
class CloudStack(groups: List<Group>, val network: Network, val security: Security, val image: Image, parameters: Map<String, String>) {

    val groups: List<Group>
    val parameters: Map<String, String>

    init {
        this.groups = ImmutableList.copyOf(groups)
        this.parameters = ImmutableMap.copyOf(parameters)
    }

    override fun toString(): String {
        val sb = StringBuilder("CloudStack{")
        sb.append("groups=").append(groups)
        sb.append(", network=").append(network)
        sb.append(", security=").append(security)
        sb.append(", image=").append(image)
        sb.append('}')
        return sb.toString()
    }
}
