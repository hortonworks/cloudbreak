package com.sequenceiq.cloudbreak.cloud.event.platform

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult
import com.sequenceiq.cloudbreak.util.JsonUtil

class ResourceDefinitionResult : CloudPlatformResult<CloudPlatformRequest<Any>> {

    val definition: String?

    constructor(request: CloudPlatformRequest<*>, definition: String) : super(request) {
        this.definition = definition
    }

    constructor(statusReason: String, errorDetails: Exception, request: CloudPlatformRequest<*>) : super(statusReason, errorDetails, request) {
        this.definition = null
    }

    override fun toString(): String {
        val sb = StringBuilder("ResourceDefinitionResult{")
        sb.append("definition='").append(JsonUtil.minify(definition)).append('\'')
        sb.append('}')
        return sb.toString()
    }
}
