package com.sequenceiq.cloudbreak.cloud.event.resource

import java.util.Collections

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance

class GetInstancesStateRequest<T> : CloudPlatformRequest<T> {

    val instances: List<CloudInstance>

    constructor(cloudContext: CloudContext, cloudCredential: CloudCredential) : super(cloudContext, cloudCredential) {
        this.instances = emptyList<CloudInstance>()
    }

    constructor(cloudContext: CloudContext, cloudCredential: CloudCredential, instances: List<CloudInstance>) : super(cloudContext, cloudCredential) {
        this.instances = instances
    }

    override fun toString(): String {
        val sb = StringBuilder("GetInstancesStateRequest{")
        sb.append(", instances=").append(instances)
        sb.append('}')
        return sb.toString()
    }
}
