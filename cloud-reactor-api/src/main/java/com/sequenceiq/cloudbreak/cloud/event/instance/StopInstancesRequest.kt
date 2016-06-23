package com.sequenceiq.cloudbreak.cloud.event.instance

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource

class StopInstancesRequest<T>(cloudContext: CloudContext, cloudCredential: CloudCredential, val resources: List<CloudResource>, val cloudInstances: List<CloudInstance>) : CloudPlatformRequest<T>(cloudContext, cloudCredential) {

    override fun toString(): String {
        val sb = StringBuilder("StopInstancesRequest{")
        sb.append("cloudInstances=").append(cloudInstances)
        sb.append(", resources=").append(resources)
        sb.append('}')
        return sb.toString()
    }
}
