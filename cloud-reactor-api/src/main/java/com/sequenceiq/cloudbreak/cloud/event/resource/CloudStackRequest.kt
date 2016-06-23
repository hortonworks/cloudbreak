package com.sequenceiq.cloudbreak.cloud.event.resource

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudStack

open class CloudStackRequest<T>(cloudContext: CloudContext, cloudCredential: CloudCredential, val cloudStack: CloudStack) : CloudPlatformRequest<T>(cloudContext, cloudCredential) {

    override fun toString(): String {
        return "CloudStackRequest{"
        +", cloudStack=" + cloudStack
        +'}'
    }
}
