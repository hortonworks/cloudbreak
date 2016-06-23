package com.sequenceiq.cloudbreak.cloud.event.instance

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus

class InstancesStatusResult(val cloudContext: CloudContext, val results: List<CloudVmInstanceStatus>) {

    override fun toString(): String {
        val sb = StringBuilder("InstancesStatusResult{")
        sb.append("cloudContext=").append(cloudContext)
        sb.append(", results=").append(results)
        sb.append('}')
        return sb.toString()
    }
}