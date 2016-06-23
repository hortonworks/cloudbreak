package com.sequenceiq.cloudbreak.cloud.event.platform

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant

class ResourceDefinitionRequest(val platform: CloudPlatformVariant, val resource: String) : CloudPlatformRequest<ResourceDefinitionResult>(null, null) {

    override fun toString(): String {
        val sb = StringBuilder("GetResourceDefinition{")
        sb.append("platform='").append(platform).append('\'')
        sb.append(", resource='").append(resource).append('\'')
        sb.append('}')
        return sb.toString()
    }
}
