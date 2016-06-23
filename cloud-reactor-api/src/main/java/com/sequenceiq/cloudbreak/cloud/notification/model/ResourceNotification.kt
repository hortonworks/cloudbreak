package com.sequenceiq.cloudbreak.cloud.notification.model

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.model.CloudResource

import reactor.rx.Promise
import reactor.rx.Promises

/**
 * Notification sent to Cloudbreak.
 */
class ResourceNotification(val cloudResource: CloudResource, val cloudContext: CloudContext, val type: ResourceNotificationType) {
    val promise: Promise<ResourcePersisted>

    init {
        this.promise = Promises.prepare<ResourcePersisted>()
    }

    val result: ResourcePersisted
        get() {
            try {
                return promise.await()
            } catch (e: InterruptedException) {
                throw CloudConnectorException("ResourceNotification has been interrupted", e)
            }

        }

    override fun toString(): String {
        val sb = StringBuilder("ResourceNotification{")
        sb.append("cloudResource=").append(cloudResource)
        sb.append(", promise=").append(promise)
        sb.append(", cloudContext=").append(cloudContext)
        sb.append(", type=").append(type)
        sb.append('}')
        return sb.toString()
    }
}
