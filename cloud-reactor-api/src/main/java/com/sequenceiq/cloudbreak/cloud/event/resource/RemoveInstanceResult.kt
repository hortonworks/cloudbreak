package com.sequenceiq.cloudbreak.cloud.event.resource

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult
import com.sequenceiq.cloudbreak.cloud.event.InstancePayload
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance

class RemoveInstanceResult : CloudPlatformResult<RemoveInstanceRequest<Any>>, InstancePayload {

    constructor(result: DownscaleStackResult, request: RemoveInstanceRequest<*>) {
        init(result.status, result.statusReason, result.errorDetails, request)
    }

    constructor(statusReason: String, errorDetails: Exception, request: RemoveInstanceRequest<Any>) : super(statusReason, errorDetails, request) {
    }

    val cloudInstance: CloudInstance?
        get() {
            val instances = request.instances
            return if (instances.isEmpty()) null else instances[0]
        }

    override val instanceId: String?
        get() {
            if (cloudInstance == null) {
                return null
            } else {
                return cloudInstance!!.instanceId
            }
        }
}
