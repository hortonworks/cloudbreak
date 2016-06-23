package com.sequenceiq.cloudbreak.cloud.event.resource

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudStack

class UpscaleStackRequest<T>(cloudContext: CloudContext, cloudCredential: CloudCredential, stack: CloudStack, val resourceList: List<CloudResource>) : CloudStackRequest<T>(cloudContext, cloudCredential, stack) {

    override fun toString(): String {
        val sb = StringBuilder("UpscaleStackRequest{")
        sb.append("resourceList=").append(resourceList)
        sb.append('}')
        return sb.toString()
    }
}
