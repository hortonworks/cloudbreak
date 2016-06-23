package com.sequenceiq.cloudbreak.cloud.notification

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotificationType
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourcePersisted

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class ResourceNotifier : PersistenceNotifier {

    @Inject
    private val eventBus: EventBus? = null

    override fun notifyAllocation(cloudResource: CloudResource, cloudContext: CloudContext): ResourcePersisted {
        val notification = ResourceNotification(cloudResource, cloudContext, ResourceNotificationType.CREATE)
        LOGGER.info("Sending resource allocation notification: {}, context: {}", notification, cloudContext)
        eventBus!!.notify("resource-persisted", Event.wrap(notification))
        return notification.result
    }

    override fun notifyUpdate(cloudResource: CloudResource, cloudContext: CloudContext): ResourcePersisted {
        val notification = ResourceNotification(cloudResource, cloudContext, ResourceNotificationType.UPDATE)
        LOGGER.info("Sending resource update notification: {}, context: {}", notification, cloudContext)
        eventBus!!.notify("resource-persisted", Event.wrap(notification))
        return notification.result
    }

    override fun notifyDeletion(cloudResource: CloudResource, cloudContext: CloudContext): ResourcePersisted {
        val notification = ResourceNotification(cloudResource, cloudContext, ResourceNotificationType.DELETE)
        LOGGER.info("Sending resource deletion notification: {}, context: {}", notification, cloudContext)
        eventBus!!.notify("resource-persisted", Event.wrap(notification))
        return notification.result
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ResourceNotifier::class.java)
    }
}
