package com.sequenceiq.cloudbreak.cloud.event.instance

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource

class StartInstancesRequest(cloudContext: CloudContext, credential: CloudCredential, val resources: List<CloudResource>, val cloudInstances: List<CloudInstance>) : CloudPlatformRequest<StartInstancesResult>(cloudContext, credential) {

    override fun toString(): String {
        val sb = StringBuilder("StartInstancesRequest{")
        sb.append("cloudInstances=").append(cloudInstances)
        sb.append(", resources=").append(resources)
        sb.append('}')
        return sb.toString()
    }
}
